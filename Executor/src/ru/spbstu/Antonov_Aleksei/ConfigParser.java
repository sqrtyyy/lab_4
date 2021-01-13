package ru.spbstu.Antonov_Aleksei;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class ConfigParser {
    private final Logger logger;
    ConfigParser(Logger logger){
        this.logger = logger;
    }

    public static String deleteSpaces(String str){
        if(str == null){
            return null;
        }
        return str.replaceAll("\\s", "");
    }

    public Map<Param, String> create_object(String file_name, ConfigParams cfg_params){
        if(file_name == null){
            logger.warning(Errors.NULL_POINTER.getErrMsg());
            return null;
        }
        FileReader fileReader;
        try {
            fileReader = new FileReader(file_name);
        } catch (FileNotFoundException e){
            logger.warning(Errors.FILE_ISNT_AVAILABLE.getErrMsg(file_name));
            return null;
        }
        Map<Param, String> params = syntax_analyze(fileReader, cfg_params);
        if(params != null){
            ErrorMessage errSemantics = semantics_analyze(params);
            if(errSemantics.isOk()){
                return null;
            }
        }
        return params;


    }

    private Map<Param, String> syntax_analyze(FileReader file_reader, ConfigParams cfg_params){
        Scanner scanner = new Scanner(file_reader);
        Map <Param, String> paramsValues = new HashMap<>();

        while (scanner.hasNext()) {
            String line = deleteSpaces(scanner.nextLine());
            String[] tokens = line.split(cfg_params.getDelimiter());
            if(tokens.length != cfg_params.getTokensNum()){
                logger.warning(Errors.WRONG_AMOUNT_OF_TOKENS.getErrMsg(line));
                return null;
            }
            boolean isParam = false;
            for (Param param : cfg_params.getNames()) {
                if(tokens[0].equals(param.toString())){
                    isParam = true;
                    if(!paramsValues.containsKey(param)){
                        paramsValues.put(param, tokens[1]);
                    } else {
                        logger.warning(Errors.PARAM_DEFINED_TWICE.getErrMsg(tokens[0]));
                        return null;
                    }
                }
            }
            if(!isParam){
                logger.warning(Errors.WRONG_PARAMETER_NAME.getErrMsg(tokens[0]));
                return null;
            }
        }
        return paramsValues;
    }

    private ErrorMessage semantics_analyze(Map<Param, String> param_values){
        for(Map.Entry<Param, String> param_value : param_values.entrySet()){
            Param param = param_value.getKey();
            String value = param_value.getValue();
            ErrorMessage error = param.isValid(value);
            if(error.isOk()) {
                logger.warning(error.getErrMsg() + ' ' + value);
                return error;
            }
        }
        return Errors.OK;
    }

    enum Errors implements ErrorMessage{
        NULL_POINTER("The null pointer was tried to use."),
        FILE_ISNT_AVAILABLE("File \"_replace_\" is not available."),
        PARAM_DEFINED_TWICE("Parameter \"_replace_\" was defined twice."),
        WRONG_PARAMETER_NAME("Parameter \"_replace_\" was not found."),
        WRONG_AMOUNT_OF_TOKENS("Line \"_replace_\" has wrong amount of tokens."),
        WRONG_CLASS_NAME("Class \"_replace_\" doesn't exist."),
        OK("OK");
        private final String errMsg;

        Errors(String errMsg){
            this.errMsg = errMsg;
        }

        public String getErrMsg(String str) {
            String stringToReplace = "_replace_";
            return errMsg.replace(stringToReplace, str);
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
