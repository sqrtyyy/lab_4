package ru.spbstu.Antonov_Aleksei;
import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Reader implements IReader  {
    private FileInputStream inputFile;
    private final Logger logger;
    private byte[] data_;
    private byte[] buffer_;
    private final TYPE[] supportedTypes = new TYPE[]{TYPE.CHAR, TYPE.BYTE};
    private final HashMap<Integer, byte[]> chunks = new HashMap<>();

    private INotifier consumer_notifier;

    public Reader(Logger logger){
        this.logger = logger;
    }
    @Override
    public RC setInputStream(FileInputStream fis) {
        inputFile = fis;
        return RC.CODE_SUCCESS;
    }

    @Override
    public RC setConfig(String cfg_src) {
        ConfigParser parser = new ConfigParser(logger);
        Map<Param, String> params = parser.create_object(cfg_src, new ReaderParams());
        if(params == null){
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        for (Map.Entry<Param, String> param_value : params.entrySet()){
            Param param = param_value.getKey();
            if(param instanceof ReaderParams.Params){
                param.setValue(this, param_value.getValue());
            } else {
                return RC.CODE_INVALID_ARGUMENT;
            }
        }
        return RC.CODE_SUCCESS;
    }
    @Override
    public RC addNotifier(INotifier iNotifier) {
        consumer_notifier = iNotifier;
        return RC.CODE_SUCCESS;
    }
    private RC execute() {
        int size;
        int chunk_number = 0;
        RC error = RC.CODE_SUCCESS;
        do {
            try {
                size = inputFile.read(buffer_);
            } catch (IOException e) {
                return RC.CODE_FAILED_TO_READ;
            }
            if(size < 0){
                data_ = null;
            } else {
                data_ = new byte[size];
                System.arraycopy(buffer_, 0, data_, 0, size);
            }
            synchronized (chunks) {
                chunks.put(chunk_number,data_);
            }
            consumer_notifier.notify(chunk_number);
            chunk_number = chunk_number + 1;
        }while (data_ != null);
        return error;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return supportedTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        return new ReaderMediator(type);
    }

    @Override
    public void run() {
        execute();
    }

    private class ReaderMediator implements IMediator{
        private final TYPE type_;
        public ReaderMediator(TYPE type){
            type_ = type;
        }
        @Override
        public Object getData(int idChunk) {
            Object return_data = null;
            synchronized (chunks) {
                data_ = chunks.get(idChunk);
                if (data_ == null) return null;
                switch (type_) {
                    case BYTE:
                        return_data = data_.clone();
                        break;
                    case CHAR:
                        return_data = new char[data_.length / 2];
                        for (int i = 0; i < ((char[]) return_data).length; i++) {
                            ((char[]) return_data)[i] = (char) ((data_[i * 2] << 8) | (data_[i * 2 + 1] & 0xff));
                        }
                        break;
                }
                chunks.remove(idChunk);
            }
            return return_data;
        }
    }
    void setBuffer(byte[] buffer){
        buffer_ = buffer;
    }
}
