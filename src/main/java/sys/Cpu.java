package sys;

public final class Cpu {
    public static int MEM_LIMIT = 4096;
    public static int REGISTERS = 16;
    public static short MEM_OFFSET = 0x200;
    public static int STACK_LIMIT = 16;

    public static short xMask(short opcode) {
        return (short) ((opcode & 0x0F00) >> 8);
    }
    public static short yMask(short opcode) {
       return (short) ((opcode & 0x00F0) >> 4);
    }
    public static short lMask(short opcode) {
        return (short) (opcode & 0x000F);
    }

    /* Memory map:
        0x000-0x1FF - CHIP 8 interpreter (contains font set in emu)
        0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
        0x200-0xFFF - Program ROM and work RAM
     */
    private byte memory[];
    private byte registers[];
    private short stack[];
    private short stackPointer;
    private short indexRegister;
    private short programCounter;

    //time registers
    private byte delay_timer;
    private byte sound_timer;

    private void init() {
        memory = new byte[MEM_LIMIT];
        registers = new byte[REGISTERS];
        stack = new short[STACK_LIMIT];
    }

    private void skipInst() {
        nextInst();
        nextInst();
    }

    private void nextInst() {
        programCounter +=2;
    }

    private int pickSingleDigitFromHex(short number, short which) {
        assert which >= 0 : "Can't be negative";
        assert which <= 4 : "Can't be greater than 4";

        short[] masks = {0x000F,0x00F0,0x0F00, (short) 0xF000};
        int num = (int)number & masks[which];
        return num;
    }

    // Do 1 cycle of fetch decode and execute
    private void fetchDecodeExecute() {
        assert programCounter + 1 < MEM_LIMIT : "Program counter has gone through memory limit!";

        // An CHIP-8 opcode is 16bit long so we have to fetch two memory addresses
        short opcode = (short) (memory[programCounter] << 8 | memory[programCounter +1]);

        //Nine single digit test instructions
        switch (opcode & 0xF000) {
            case 0x1000: //1nnn
                // jumps to nnn
                programCounter = (short) (MEM_OFFSET +  (opcode & 0x0FFF));
                break;
            case 0x2000: //2nnn
                // calls subroutine at nnn
                stack[stackPointer] =  programCounter;
                ++programCounter;
                programCounter = (short) (MEM_OFFSET + (opcode & 0x0FFF));
                break;
            case 0x3000: //3xnn
                // skips next if vx equals nn
                if ((registers[xMask(opcode)]) == (opcode & 0x00FF))
                    skipInst();
                break;
            case 0x4000: //4xnn
                // skips next if vx doesn't equal nn
                if ((registers[xMask(opcode)]) != (opcode & 0x00FF))
                    skipInst();
                break;
            case 0x5000: //5xy0
                // skips next instruction if vx equals vy
                if ((registers[xMask(opcode)]) == (registers[yMask(opcode)]))
                    skipInst();
                break;
            case 0x6000: //6xnn
                // sets vx to nn
                registers[xMask(opcode)] = (byte) (opcode & 0x00FF);
                break;
            case 0x7000: //7xnn
                // adds nn to vx
                registers[xMask(opcode)] += (byte) (opcode & 0x00FF);
                break;
            case 0x8000: //8xy0
                // sets vx to value of vy
                registers[xMask(opcode)] = registers[yMask(opcode)];
                break;
            case 0x9000: //9xy0
                // Skips the next instruction if VX doesn't equal VY.
                if ((registers[xMask(opcode)]) != registers[yMask(opcode)])
                    skipInst();
                break;
        }

        //Nine double digit test instructions
        switch (opcode & 0xF00F) {
            case 0x8001: //Sets VX to VX or VY. (Bitwise OR operation)
                break;

            case 0x8002: //Sets VX to VX and VY.
                break;

            case 0x8003: // Sets VX to VX
                break;

            case 0x8004:
                break;

            case 0x8005:
                break;

            case 0x8006:
                break;

            case 0x8007:
                break;

            case 0x800E:
                break;

        }



    }


}
