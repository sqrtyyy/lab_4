package ru.spbstu.Antonov_Aleksei;

public class WriterParams implements ConfigParams {
    private static final String delimiter = "=";
    private static final Param[] names = {Params.SIZE};
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
        INPUT_FILE("input_file"),
        SIZE("SIZE");
        String name;

        Params(String name){
            this.name = name;
        }
        @Override
        public ErrorMessage isValid(String value) {
            Errors msg = Errors.OK;
            switch (this){
                case SIZE:
                    try {
                        Integer.parseInt(value);
                    } catch (NumberFormatException e){
                        msg = Errors.WRONG_SIZE_FORMAT;
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
            if(obj instanceof Writer){
                Writer writer = (Writer)obj;
                switch (this){
                    case SIZE:
                        byte[] b = new byte[Integer.parseInt(value)];
                        writer.setBuffer(b);
                    case INPUT_FILE:

                }
            }
        }
    }
    enum Errors implements ErrorMessage {
        WRONG_SIZE_FORMAT("Wrong format of size.\nExample:  124"),
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
