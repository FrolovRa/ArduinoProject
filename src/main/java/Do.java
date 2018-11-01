public enum Do {

    VALVE_OUT_ON('1'),
    VALVE_OUT_OFF('0'),
    PUMP_FOR_TITRATION_ON('3'),
    PUMP_FOR_TITRATION_OFF('2'),
    PUMP_FOR_SOLUTION_ON('5'),
    PUMP_FOR_SOLUTION_OFF('4'),
    MIXER_ON('7'),
    MIXER_OFF('6'),
    VALVE_SOLUTION_ON('9'),
    VALVE_SOLUTION_OFF('8'),
    VALVE_TITRATION_ON('B'),
    VALVE_TITRATION_OFF('A'),
    VALVE_WATER_ON('D'),
    VALVE_WATER_OFF('C'),
    PH_METER_ON('Y'),
    PH_METER_OFF('X');

    private char c;

    Do(char c) {
        this.c = c;
    }
    public char getChar() {
        return c;
    }
}
