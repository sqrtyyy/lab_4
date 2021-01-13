package ru.spbstu.Antonov_Aleksei;


public class XOR_Params implements ConfigParams {
    private static final String delimiter = "=";
    private static final Param[] names = {Params.KEY};
    private static final int tokensNum = 2;

    @Override
    public Param[] getNames() {
        return names;
    }

    @Override
    public int getTokensNum() {
        return tokensNum;
    }

    @Override
    public String getDelimiter() {
        return delimiter;
    }

    enum Params implements Param {
        KEY("KEY");
        String name;

        Params(String name){
            this.name = name;
        }
        @Override
        public ErrorMessage isValid(String value) {
            Errors msg = Errors.OK;
            switch (this){
                case KEY:
                    String numDelimiter = ",";
                    try {
                        String[] nums = value.split(numDelimiter);
                        for (String num: nums) {
                            Short.parseShort(num);
                        }
                    } catch (NumberFormatException e){
                        msg = Errors.WRONG_KEY_FORMAT;
                    }
                    break;

            }
            return msg;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public void setValue(Object obj, String value) {
            if(obj instanceof XOR_Encoder){
                XOR_Encoder xor_encoder = (XOR_Encoder)obj;
                switch (this){
                    case KEY:
                        String numDelimiter = ",";
                        String[] nums = value.split(numDelimiter);
                        short[] key = new short[nums.length];
                        for (int i = 0; i < nums.length; i++) {
                            key[i] = Short.parseShort(nums[i]);
                        }
                        xor_encoder.setKey(key);

                }
            }
        }
    }
    enum Errors implements ErrorMessage {
        WRONG_KEY_FORMAT("Wrong format of key.\nExample:  124"),
        OK("OK");
        private final String errMsg;

        Errors(String errMsg){
            this.errMsg = errMsg;
        }
        @Override
        public boolean isOk() {
            return !this.equals(OK);
        }

        public String getErrMsg() {
            return errMsg;
        }
    }
}
