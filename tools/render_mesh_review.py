"""Render the production mesh/rig/shader export with Mesa EGL (pip install moderngl pillow numpy).
Run after gradle testDebugUnitTest. This is visual QA, not an Android device test.
"""
import json
from pathlib import Path
import moderngl
import numpy as np
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
folder=root/'android/app/build/astra-previews'
data=json.loads((folder/'mesh-review.json').read_text())
ctx=moderngl.create_standalone_context(backend='egl')
# Only the GLSL desktop declaration syntax changes; shading math is production code.
v='#version 330\n'+data['vertex'].replace('attribute ','in ').replace('varying ','out ')
f='#version 330\nout vec4 fragColor;\n'+data['fragment'].replace('precision mediump float;','').replace('varying ','in ').replace('gl_FragColor','fragColor')
program=ctx.program(vertex_shader=v,fragment_shader=f)
meshes=[]
for mesh in data['meshes']:
    buf=ctx.buffer(np.column_stack([np.array(mesh['p']).reshape(-1,3),np.array(mesh['n']).reshape(-1,3)]).astype('f4').tobytes())
    meshes.append(ctx.vertex_array(program,[(buf,'3f 3f','aPosition','aNormal')]))
size=600
frame=ctx.simple_framebuffer((size,size),components=4);frame.use()
ctx.enable(moderngl.DEPTH_TEST|moderngl.CULL_FACE);ctx.depth_func='<='
images=[]
for pose in data['poses']:
    frame.clear(.86,.87,.87,1,depth=1)
    ctx.front_face='ccw' if pose['mirrored'] else 'cw'
    program['uViewProjection'].write(np.array(pose['vp'],dtype='f4').tobytes())
    for part in pose['parts']:
        program['uModel'].write(np.array(part['matrix'],dtype='f4').tobytes())
        program['uColor'].value=tuple(part['color'])
        ctx.cull_face='front';program['uOutline'].value=1;program['uOutlineWidth'].value=.45
        meshes[part['mesh']].render()
        ctx.cull_face='back';program['uOutline'].value=0;program['uOutlineWidth'].value=0
        meshes[part['mesh']].render()
    img=Image.frombytes('RGBA',(size,size),frame.read(components=4)).transpose(Image.Transpose.FLIP_TOP_BOTTOM).convert('RGB')
    assert np.count_nonzero(np.asarray(img)[:,:,0]<180)>1000, 'Empty model render'
    ImageDraw.Draw(img).text((22,20),pose['name'],fill=(20,30,45),font_size=22)
    images.append(img)
    img.save(folder/(pose['name'].lower()+'.png'))
sheet=Image.new('RGB',(size*4,size*4),(219,222,222))
for i,img in enumerate(images):sheet.paste(img,((i%4)*size,(i//4)*size))
sheet.save(folder/'prototype01-poses.png')
print(ctx.info['GL_RENDERER'],f"— {len(images)} poses, {sum(len(m['p'])//9 for m in data['meshes'])} unique triangles")
