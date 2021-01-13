package ru.spbstu.Antonov_Aleksei;

import ru.spbstu.pipeline.IExecutor;
import ru.spbstu.pipeline.IReader;
import ru.spbstu.pipeline.IWriter;
import ru.spbstu.pipeline.RC;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.logging.Logger;


public class Manager {
    private FileInputStream inputStream;
    private FileOutputStream fileOutputStream;
    private IReader reader;
    private IWriter writer;
    private IExecutor[] executors;
    public final Logger logger;

    RC error = RC.CODE_SUCCESS;

    public Manager(Logger logger){
        this.logger = logger;
    }

    public void setExecutors(IExecutor[] executors) {
        this.executors =executors;
    }

    public void setFileOutputStream(String fileName) {
        try {
            fileOutputStream = new FileOutputStream(fileName);
        } catch (FileNotFoundException ignored) {
        }
    }

    public void setInputStream(String fileName) {
        try {
            inputStream = new FileInputStream(fileName);
        } catch (FileNotFoundException ignored) {
        }
    }

    public void setReader(IReader iReader) {
        this.reader = iReader;
    }

    public void setWriter(IWriter iWriter) {
        this.writer = iWriter;
    }

    public RC setConfig(String file){
        ConfigParser parser = new ConfigParser(logger);
        Map<Param, String> params = parser.create_object(file, new ManagerParams());
        if(params == null){
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        for (Map.Entry<Param, String> param_value : params.entrySet()){
            Param param = param_value.getKey();
            if(param instanceof ManagerParams.Params){
                param.setValue(this, param_value.getValue());
                if(!error.equals(RC.CODE_SUCCESS)){
                    return error;
                }
            } else {
                return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
            }
        }
        return RC.CODE_SUCCESS;
    }

    public void setPipeLine(){
        if(inputStream == null || executors == null
                || fileOutputStream == null || reader == null) return;
        reader.setInputStream(inputStream);
        reader.addNotifier(executors[0].getNotifier());
        writer.setOutputStream(fileOutputStream);
        writer.setProducer(executors[executors.length - 1]);
        for (int i = 0; i < executors.length; i++) {
            if(i != executors.length - 1) {
                executors[i].addNotifier(executors[i + 1].getNotifier());
            }
            if(i != 0){
                executors[i].setProducer(executors[i - 1]);
            }
        }
        executors[0].setProducer(reader);
        executors[executors.length - 1].addNotifier(writer.getNotifier());
    }
    public void run(){
        Thread[] threads = new Thread[1 + executors.length + 1];
        threads[0] = new Thread(reader);
        for (int i = 0; i < executors.length; i++) {
            threads[i + 1] = new Thread(executors[i]);
        }
        threads[threads.length - 1] = new Thread(writer);

        for (Thread thread: threads) {
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
