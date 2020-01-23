package sys;

public final class Gfx {
    //  2048 pixels
    public static int MEM_LIMIT = 64*32;
    private char[] memory;

    private void init() {
        memory = new char[MEM_LIMIT];
    }
}
