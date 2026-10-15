package android.view;

/** PC: só o nome (o tipo do controle que o SDL informa, ex.: "XBOXONE", "PS5"). */
public final class InputDevice {
    private final String name;

    public InputDevice(String name) { this.name = name; }

    public String getName() { return name; }
}
