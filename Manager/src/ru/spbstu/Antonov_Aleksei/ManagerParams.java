package ru.spbstu.Antonov_Aleksei;

import ru.spbstu.pipeline.IExecutor;
import ru.spbstu.pipeline.IReader;
import ru.spbstu.pipeline.IWriter;
import ru.spbstu.pipeline.RC;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public class ManagerParams implements ConfigParams {
    private static final String delimiter = "=";
    private static final Param[] names = {Params.READER, Params.EXECUTORS, Params.INPUT_FILE, Params.WRITER, Params.OUTPUT_FILE};
    private static final int tokensNum = 2;
    private static final String split_classes = "/__/";
    private static final String split_class_and_cfg = "//";
    @Override
    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public Param[] getNames() {
        return names;
    }

    @Override
    public int getTokensNum() {
        return tokensNum;
    }

    enum Params implements Param {
        INPUT_FILE("INPUT_FILE"),
        OUTPUT_FILE("OUTPUT_FILE"),
        READER("READER_CLASS"),
        WRITER("WRITER_CLASS"),
        EXECUTORS("EXECUTORS");
        String name;

        Params(String name) {
            this.name = name;
        }

        @Override
        public ErrorMessage isValid(String value) {
            Errors msg = Errors.OK;
            switch (this) {
                case INPUT_FILE:
                    if (!new File(value).isFile()) {
                        msg = Errors.WRONG_INPUT_FILE;
                    }
                    break;
                case READER:
                case WRITER:{
                    String[] tokens = value.split(split_class_and_cfg);
                    int tokens_num = 2;
                    if (tokens.length != tokens_num) {
                        msg = Errors.WRONG_FORMAT;
                        break;
                    }
                    if(!tokens[1].startsWith("\"") || !tokens[1].endsWith("\"") ){
                        msg = Errors.WRONG_FORMAT;
                        break;
                    }

                    try {
                        Class.forName(tokens[0]);
                    } catch (ClassNotFoundException e) {
                        msg = Errors.WRONG_CLASS_NAME;
                        break;
                    }

                    try {
                        Class iFace = this.equals(READER) ?  IReader.class : IWriter.class;
                        if(IsImplements(Class.forName(tokens[0]), iFace)){
                            msg = this.equals(READER) ? Errors.DOESNT_IMPLEMENT_IREADER : Errors.DOESNT_IMPLEMENT_IWRITER;
                        }
                        String fileName = tokens[1].substring(1, tokens[1].length() - 1);
                        if (!new File(fileName).isFile()) {
                            msg = Errors.WRONG_FORMAT;
                            break;
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                    break;
                }
                case EXECUTORS:{
                    String[] classes = value.split(split_classes);
                    for (String aClass : classes) {
                        String[] tokens = aClass.split(split_class_and_cfg);
                        int tokens_num = 2;
                        if (tokens.length != tokens_num) {
                            msg = Errors.WRONG_FORMAT;
                            break;
                        }
                        try {
                            Class.forName(tokens[0]);
                        } catch (ClassNotFoundException e) {
                            msg = Errors.WRONG_CLASS_NAME;
                            break;
                        }
                        try {
                            Class iFace = IExecutor.class;
                            if (IsImplements(Class.forName(tokens[0]), iFace)) {
                                msg = Errors.DOESNT_IMPLLEMENT_IEXECUTOR;
                                break;
                            }
                        } catch (ClassNotFoundException ignored) { }
                        String fileName = tokens[1].substring(1, tokens[1].length() - 1);
                        if (!new File(fileName).isFile()) {
                            msg = Errors.WRONG_FORMAT;
                            break;
                        }
                    }
                    break;
                }

            }
            return msg;
        }

        private boolean IsImplements(Class clazz, Class iFace){
            Class[] ifaces = clazz.getInterfaces();
            for (Class iface : ifaces){
                if(iface.equals(iFace)) return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public void setValue(Object obj, String value) {
            if(obj instanceof Manager){
                Manager manager = (Manager) obj;
                switch (this){
                    case INPUT_FILE:
                        manager.setInputStream(value);
                        break;
                    case EXECUTORS: {
                        String[] classes = value.split(split_classes);
                        IExecutor[] executors = new IExecutor[classes.length];
                        for (int i = 0; i < classes.length; i++) {
                            String[] tokens = classes[i].split(split_class_and_cfg);
                            if (tokens.length != 2) continue;
                            IExecutor executor = null;
                            try {
                                try {
                                    String fileName = tokens[1].substring(1, tokens[1].length() - 1);
                                    Constructor<?> constructor = Class.forName(tokens[0]).getDeclaredConstructor(Logger.class);
                                    executor = (IExecutor) constructor.newInstance(manager.logger);
                                    manager.error = executor.setConfig(fileName);
                                    if(!manager.error.equals(RC.CODE_SUCCESS)){
                                        manager.logger.warning(Errors.WRONG_CONFIG.getErrMsg());
                                    }
                                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException ignored) { }
                            } catch (ClassNotFoundException ignored) {
                            }
                            executors[i] = executor;

                        }
                        manager.setExecutors(executors);
                        break;
                    }
                    case OUTPUT_FILE:{
                        manager.setFileOutputStream(value);
                        break;
                    }
                    case READER:{
                        IReader reader = null;
                        String[] tokens = value.split(split_class_and_cfg);
                        try {
                            try {
                                String fileName = tokens[1].substring(1, tokens[1].length() - 1);
                                reader = (IReader) Class.forName(tokens[0]).getConstructor(Logger.class).newInstance(manager.logger);
                                manager.error = reader.setConfig(fileName);
                                if(!manager.error.equals(RC.CODE_SUCCESS)){
                                    manager.logger.warning(Errors.WRONG_CONFIG.getErrMsg());
                                }
                            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException ignored) {
                            }
                        } catch (ClassNotFoundException ignored) {
                        }
                        manager.setReader(reader);
                        break;
                    }
                    case WRITER:{
                        IWriter writer = null;
                        String[] tokens = value.split(split_class_and_cfg);
                        try {
                            try {
                                String fileName = tokens[1].substring(1, tokens[1].length() - 1);
                                writer = (IWriter) Class.forName(tokens[0]).getConstructor(Logger.class).newInstance(manager.logger);
                                manager.error = writer.setConfig(fileName);
                                if(!manager.error.equals(RC.CODE_SUCCESS)){
                                    manager.logger.warning(Errors.WRONG_CONFIG.getErrMsg());
                                }
                            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException ignored) {
                            }
                        } catch (ClassNotFoundException ignored) {
                        }
                        manager.setWriter(writer);
                    }

                }
            }
        }

        enum Errors implements ErrorMessage {
            DOESNT_IMPLEMENT_IREADER("Doesn't implement IReader"),
            DOESNT_IMPLEMENT_IWRITER("Doesn't implement IWriter"),
            DOESNT_IMPLLEMENT_IEXECUTOR("Doesn't implement IExecuror"),
            WRONG_FORMAT("Wrong format of field"),
            WRONG_CLASS_NAME("Wrong class name"),
            WRONG_INPUT_FILE("File does not exist"),
            WRONG_CONFIG("Wrong config"),
            OK("OK");

            private final String errMsg;

            Errors(String errMsg) {
                this.errMsg = errMsg;
            }

            public String getErrMsg() {
                return errMsg;
            }

            @Override
            public boolean isOk() {
                return !this.equals(OK);
            }
        }
    }
}
