package sys;

public final class Chip8 {
    public static int MEM_LIMIT = 4096;
    public static int REGISTERS = 16;
    public static short MEM_OFFSET = 0x200;
    public static int STACK_LIMIT = 16;
    public static int GMEM_LIMIT = 64*32;
    public static int KEYBOARD_KEYS = 16;

    public static short xMask(short opcode) {
        return (short) ((opcode & 0x0F00) >> 8);
    }
    public static short yMask(short opcode) {
       return (short) ((opcode & 0x00F0) >> 4);
    }

    /* Memory map:
        0x000-0x1FF - CHIP 8 interpreter (contains font set in emu)
        0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
        0x200-0xFFF - Program ROM and work RAM
     */
    private byte[] memory;
    private byte[] gmemory;
    private byte[] registers;
    private short[] stack;
    private boolean[] keyboardState;
    private short stackPointer;
    private short indexRegister;
    private short programCounter;

    //time registers
    private byte delay_timer;
    private byte sound_timer;

    Prng prng;

    private void init() {
        memory = new byte[MEM_LIMIT];
        gmemory = new byte[GMEM_LIMIT];
        registers = new byte[REGISTERS];
        stack = new short[STACK_LIMIT];
        keyboardState = new boolean[KEYBOARD_KEYS];
        prng = new JavaPrng();
    }

    private void nextInst() {
        programCounter +=2;
    }

    private short fetch() {
        assert programCounter + 1 < MEM_LIMIT : "Program counter has gone through memory limit!";

        // An CHIP-8 opcode is 16bit long so we have to fetch two memory addresses
        return (short) (memory[programCounter] << 8 | memory[programCounter +1]);
    }

    private void decodeExecute(short opcode) {

        /* Opcodes
            00E0 - CLS
            00EE - RET
         */
        switch (opcode) {
            case 0x00E0:
                break;
            case  0x00EE:
                programCounter = stack[stackPointer];
                stack[stackPointer] = 0;
                stackPointer--;
                break;
        }

        /*
            0nnn - SYS addr
            1nnn - JP addr
            2nnn - CALL addr
            3xkk - SE Vx, byte
            4xkk - SNE Vx, byte
            5xy0 - SE Vx, Vy
            6xkk - LD Vx, byte
            7xkk - ADD Vx, byte
            Annn - LD I, addr
            Bnnn - JP V0, addr
            Cxkk - RND Vx, byte
            Dxyn - DRW Vx, Vy, nibble
            */
        switch (opcode & 0xF000) {
            case 0x0000:
                //This instruction is only used on the old computers on which Chip-8 was originally implemented.
                // It is ignored by modern interpreters.
                break;
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
                    nextInst();
                break;
            case 0x4000: //4xnn
                // skips next if vx doesn't equal nn
                if ((registers[xMask(opcode)]) != (opcode & 0x00FF))
                    nextInst();
                break;
            case 0x5000: //5xy0
                // skips next instruction if vx equals vy
                if ((registers[xMask(opcode)]) == (registers[yMask(opcode)]))
                    nextInst();
                break;
            case 0x6000: //6xnn
                // sets vx to nn
                registers[xMask(opcode)] = (byte) (opcode & 0x00FF);
                break;
            case 0x7000: //7xnn
                // adds nn to vx
                // TODO: carry 1?
                registers[xMask(opcode)] += (opcode & 0x00FF);
                break;
            case 0x8000: //8xy0
                // sets vx to value of vy
                registers[xMask(opcode)] = registers[yMask(opcode)];
                break;
            case 0x9000: //9xy0
                // Skips the next instruction if VX doesn't equal VY.
                if ((registers[xMask(opcode)]) != registers[yMask(opcode)])
                    nextInst();
                break;

            case 0xA000: //Annn The value of register I is set to nnn
                indexRegister = (short) (opcode & 0x0FFF);
                break;
            case 0xB000: //Bnnn Jump to location nnn + V0.
                programCounter = (short) ((opcode & 0x0FFF) + registers[0]);
                break;
            case 0xC000: //Cxkk Set Vx = random byte AND kk
                registers[xMask(opcode)] = (byte) (prng.generate() & ((byte) (opcode & 0x00FF)));
                break;
            case 0xD000: //Dxyn
                /* TODO
                Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision.
                The interpreter reads n bytes from memory, starting at the address stored in I.
                These bytes are then displayed as sprites on screen at coordinates (Vx, Vy).
                Sprites are XORed onto the existing screen. If this causes any pixels to be erased, VF is set to 1,
                 otherwise it is set to 0.
                If the sprite is positioned so part of it is outside the coordinates of the display,
                 it wraps around to the opposite side of the screen. See instruction 8xy3 for more information on XOR,
                  and section 2.4, Display, for more information on the Chip-8 screen and sprites.
                 */
                break;
        }
        // TODO test
        /*
            8xy0 - LD Vx, Vy
            8xy1 - OR Vx, Vy
            8xy2 - AND Vx, Vy
            8xy3 - XOR Vx, Vy
            8xy4 - ADD Vx, Vy
            8xy5 - SUB Vx, Vy
            8xy6 - SHR Vx {, Vy}
            8xy7 - SUBN Vx, Vy
            8xyE - SHL Vx {, Vy}
          */
        switch (opcode & 0xF00F) {
            case 0x8001: //8xy1 Sets VX to VX or VY. (Bitwise OR operation)
                registers[xMask(opcode)] |= registers[yMask(opcode)];
                break;

            case 0x8002: //Sets VX to VX and VY. (Bitwise AND operation)
                registers[xMask(opcode)] &= registers[yMask(opcode)];
                break;

            case 0x8003: // Sets VX to VX xor VY.
                registers[xMask(opcode)] ^= registers[yMask(opcode)];
                break;

            case 0x8004: //Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't.
                // TODO: challenge: do without conditional
                short s = (short) (registers[xMask(opcode)] + registers[yMask(opcode)]);
                if ((s&0xFF00)!= 0) registers[0xF] = 1;
                else registers[0xF] = 0;
                registers[xMask(opcode)] = (byte) (s & 0x00FF);
                break;

            case 0x8005: //VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
                // TODO: challenge: do without conditional
                if (registers[xMask(opcode)] > registers[yMask(opcode)]) registers[0xF] = 1;
                else registers[0xF] = 0;
                registers[xMask(opcode)] -= registers[yMask(opcode)];
                break;

            case 0x8006: //8xy6
                // If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2.
                registers[0xF] = (byte) (opcode & 0x0001);
                registers[xMask(opcode)] >>= 1;
                break;
            /*
                8xy7 - SUBN Vx, Vy
                Set Vx = Vy - Vx, set VF = NOT borrow.
                If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted from Vy, and the results stored in Vx.
             */
            case 0x8007:
                // TODO: challenge: do without conditional
                if (registers[yMask(opcode)] > registers[xMask(opcode)]) registers[0xF] = 1;
                else registers[0xF] = 0;
                registers[xMask(opcode)] = (byte) (registers[yMask(opcode)] - registers[xMask(opcode)]);
                break;
            /*
            8xyE - SHL Vx {, Vy}
            Set Vx = Vx SHL 1.
            If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is multiplied by 2. */
            case 0x800E:
                // TODO: challenge: do without conditional
                if((registers[xMask(opcode)] & 0x8000) != 0) registers[0xF] = 1;
                else registers[0xF] = 0;
                registers[xMask(opcode)] <<= 1;
                break;
        }

        // TODO test
        /*
            Ex9E - SKP Vx
            ExA1 - SKNP Vx
            Fx07 - LD Vx, DT
            Fx0A - LD Vx, K
            Fx15 - LD DT, Vx
            Fx18 - LD ST, Vx
            Fx1E - ADD I, Vx
            Fx29 - LD F, Vx
            Fx33 - LD B, Vx
            Fx55 - LD [I], Vx
            Fx65 - LD Vx, [I]
        */
        switch (opcode & 0xF0FF) {
            case 0xE09E:
                /*
                    Skip next instruction if key with the value of Vx is pressed.
                    Checks the keyboard, and if the key corresponding to the value of Vx is currently in
                     the down position, PC is increased by 2.
                 */
                if (keyboardState[registers[xMask(opcode)]]) nextInst();
                break;

            case 0xE0A1:
                if (!keyboardState[registers[xMask(opcode)]]) nextInst();
                break;

            case 0xF007:
                /*  Set Vx = delay timer value.
                    The value of DT is placed into Vx. */
                registers[opcode] = delay_timer;
                break;

            case 0xF00A:
                /*  Wait for a key press, store the value of the key in Vx.
                    All execution stops until a key is pressed, then the value of that key is stored in Vx. */
                int count = 0;
                while (!keyboardState[count]) {
                    count++;
                    if (count == KEYBOARD_KEYS) count = 0;
                }
                registers[xMask(opcode)] = (byte) count;
                break;

            case 0xF015:
                delay_timer = registers[xMask(opcode)];
                break;

            case 0xF018:
                sound_timer = registers[xMask(opcode)];
                break;

            case 0xF01E:
                indexRegister += registers[xMask(opcode)];
                break;

            case 0xF029:
                // TODO: Set I = location of sprite for digit Vx.
                break;

            case 0xF033:
                /* TODO
                    Store BCD representation of Vx in memory locations I, I+1, and I+2.
                    The interpreter takes the decimal value of Vx, and places the hundreds digit in memory at location in I,
                     the tens digit at location I+1, and the ones digit at location I+2.
                 */
                break;

            case 0xF055:
                /*
                    Store registers V0 through Vx in memory starting at location I.
                    The interpreter copies the values of registers V0 through Vx into memory,
                     starting at the address in I.
                 */
                for(int i = 0; i < xMask(opcode); ++i) memory[indexRegister + i] = registers[i];
                break;

            case 0xF065:
                /*Read registers V0 through Vx from memory starting at location I.
                    The interpreter reads values from memory starting at location I into registers V0 through Vx. */
                for(int i = 0; i < xMask(opcode); ++i) registers[i] = memory[indexRegister + i] ;
                break;

        }
    }

    public Chip8() {
        init();
    }

    //TODO: simulate original running scheme?
    public void cycle() {
        decodeExecute(fetch());
    }
}
