package ru.spbstu.Antonov_Aleksei;

class ReaderParams implements ConfigParams {
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
            if(obj instanceof Reader){
                Reader reader = (Reader)obj;
                switch (this){
                    case SIZE:
                        byte[] b = new byte[Integer.parseInt(value)];
                        reader.setBuffer(b);
                }
            }
        }
    }
    enum Errors implements ErrorMessage {
        WRONG_SIZE_FORMAT("Wrong format of key.\nExample:  124"),
        OK("OK");
        private final String errMsg;

        Errors(String errMsg){
            this.errMsg = errMsg;
        }
        public boolean isOk() {
            return !this.equals(OK);
        }
        public String getErrMsg() {
            return errMsg;
        }
    }


}

