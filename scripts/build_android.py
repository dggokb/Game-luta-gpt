#!/usr/bin/env python3
"""Build a signed offline APK with Android SDK tools and the installed Java VM.

No Gradle, third-party Android library, network permission or embedded credential.
The preview signing key remains outside the repository.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]


def run(command, env=None):
    result=subprocess.run([str(c) for c in command],capture_output=True,text=True,env=env)
    if result.returncode:
        raise RuntimeError(f'{Path(str(command[0])).name} falhou:\n{result.stdout}\n{result.stderr}')
    return result.stdout+result.stderr


def build(assets, android_jar, build_tools, output, work):
    assets,android_jar,build_tools,output,work=[Path(p).resolve() for p in (assets,android_jar,build_tools,output,work)]
    if output.exists():raise ValueError('Use um nome de APK ainda inexistente')
    if work.exists() and any(work.iterdir()):raise ValueError('Use uma pasta de compilação nova ou vazia')
    manifest=json.loads((assets/'manifest.json').read_text())
    if not (assets/'index.html').exists():raise ValueError('Assets incompletos')
    expected=sum(len(c['frames']) for c in manifest['clips'])
    for c in manifest['clips']:
        for f in c['frames']:
            for key in ('raw_file','normalized_file'):
                if key in f and not (assets/f[key]).is_file():raise ValueError(f'Frame ausente: {f[key]}')
    work.mkdir(parents=True,exist_ok=True)
    output.parent.mkdir(parents=True,exist_ok=True)
    run([build_tools/'aapt2','compile','--dir',ROOT/'android/res','-o',work/'resources.zip'])
    run([build_tools/'aapt2','link','-o',work/'unsigned.apk','-I',android_jar,
         '--manifest',ROOT/'android/AndroidManifest.xml','-A',assets,work/'resources.zip'])
    classes=work/'classes';classes.mkdir()
    compiler=run(['java','com.sun.tools.javac.Main','-source','8','-target','8','-bootclasspath',android_jar,
                  '-d',classes,ROOT/'android/src/org/spriteviewer/offline/MainActivity.java'])
    dex=work/'dex';dex.mkdir()
    run(['java','-cp',build_tools/'lib/d8.jar','com.android.tools.r8.D8','--min-api','26',
         '--lib',android_jar,'--output',dex,*sorted(classes.rglob('*.class'))])
    with zipfile.ZipFile(work/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(dex/'classes.dex','classes.dex')
    run([build_tools/'zipalign','-f','-p','4',work/'unsigned.apk',work/'aligned.apk'])
    env=os.environ.copy();env['SPRITE_PREVIEW_KEY_PASSWORD']=secrets.token_hex(24)
    key=work/'preview-signing.p12'
    run(['keytool','-genkeypair','-keystore',key,'-storetype','PKCS12','-alias','sprite-preview',
         '-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=Sprite Viewer Preview',
         '-storepass:env','SPRITE_PREVIEW_KEY_PASSWORD','-keypass:env','SPRITE_PREVIEW_KEY_PASSWORD'],env)
    key.chmod(0o600)
    # Retain the random password privately alongside the key for a later local update.
    password=work/'signing-password';password.write_text(env['SPRITE_PREVIEW_KEY_PASSWORD']);password.chmod(0o600)
    run([build_tools/'apksigner','sign','--ks',key,'--ks-key-alias','sprite-preview',
         '--ks-pass','env:SPRITE_PREVIEW_KEY_PASSWORD','--key-pass','env:SPRITE_PREVIEW_KEY_PASSWORD',
         '--v1-signing-enabled','true','--v2-signing-enabled','true','--v3-signing-enabled','true',
         '--out',output,work/'aligned.apk'],env)
    signature=run([build_tools/'apksigner','verify','--verbose','--print-certs',output])
    run([build_tools/'zipalign','-c','-p','4',output])
    badging=run([build_tools/'aapt','dump','badging',output])
    permissions=run([build_tools/'aapt','dump','permissions',output])
    if 'uses-permission:' in permissions:raise RuntimeError('O visualizador offline não deve solicitar permissões')
    with zipfile.ZipFile(output) as archive:
        if archive.testzip() is not None:raise RuntimeError('APK corrompido')
        bundled=json.loads(archive.read('assets/manifest.json'))
        assert sum(len(c['frames']) for c in bundled['clips'])==expected
        assert 'classes.dex' in archive.namelist()
    report={'apk':output.name,'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),
            'application_id':'org.spriteviewer.offline','minimum_android':'8.0 (API 26)',
            'offline':True,'requested_permissions':[],'clips':len(manifest['clips']),'frames':expected,
            'signature_verification':signature,'badging':badging,'compiler_notes':compiler,
            'tested_on_physical_device':False,'mobile_browser_tested_separately':True,
            'preview':manifest['mobile_preview']}
    output.with_suffix('.json').write_text(json.dumps(report,indent=2,ensure_ascii=False))
    print(json.dumps({k:report[k] for k in ('apk','bytes','sha256','frames','requested_permissions')},indent=2))
    return report


def main():
    p=argparse.ArgumentParser();p.add_argument('--assets',type=Path,required=True);p.add_argument('--android-jar',type=Path,required=True)
    p.add_argument('--build-tools',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--work',type=Path,required=True)
    args=p.parse_args();build(args.assets,args.android_jar,args.build_tools,args.output,args.work)


if __name__=='__main__':main()
