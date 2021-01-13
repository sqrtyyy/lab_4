import ru.spbstu.pipeline.RC;

import java.util.logging.Logger;

public class TestManager {
    public static final Logger logger = Logger.getLogger(TestManager.class.getName());

    public static void main(String[] args){
        if(args == null || args.length != 1){
            logger.warning(RC.CODE_INVALID_ARGUMENT.toString());
        } else {
            ru.spbstu.Antonov_Aleksei.Manager manager = new ru.spbstu.Antonov_Aleksei.Manager(logger);
            if(manager.setConfig(args[0]).equals(RC.CODE_SUCCESS)) {
                manager.setPipeLine();
                manager.run();
            }
        }
    }
}
