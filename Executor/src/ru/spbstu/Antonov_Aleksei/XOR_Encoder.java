package ru.spbstu.Antonov_Aleksei;

import ru.spbstu.pipeline.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class XOR_Encoder implements IExecutor {

    private short[] key;
    private int currentPos = 0;
    private final Logger logger;
    private IMediator producerMediator;
    private final TYPE[] supported_types = new TYPE[]{TYPE.CHAR, TYPE.SHORT};
    private TYPE producer_data_type;
    private short[] data_ = new short[0];

    private final HashMap<Integer, short[]> chunks = new HashMap<>();
    private final ArrayList<Integer> available_chunks_ids = new ArrayList<>();
    private INotifier consumer_notifier;

    int idx = 0;

    public XOR_Encoder(Logger logger){
        this.logger = logger;
    }
    @Override
    public RC setConfig(String cfg_src) {
        ConfigParser parser = new ConfigParser(logger);
        Map<Param, String> params = parser.create_object(cfg_src, new XOR_Params());
        if(params == null) return RC.CODE_CONFIG_GRAMMAR_ERROR;
        for (Map.Entry<Param, String> param_value : params.entrySet()){
            Param param = param_value.getKey();
            if(param instanceof XOR_Params.Params){
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

    @Override
    public RC setProducer(IProducer p) {
        for(TYPE type : p.getOutputTypes()){
            for (TYPE supported_type: supported_types){
                if(type.equals(supported_type)){
                    producerMediator = p.getMediator(type);
                    producer_data_type = type;
                    break;
                }
            }
            if(producerMediator != null) break;
        }
        return producerMediator != null ? RC.CODE_SUCCESS : RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
    }
    private RC execute() {
        int idx_;
        synchronized (available_chunks_ids) {
            idx_ = available_chunks_ids.get(idx);
        }
        short[] data = convert(producerMediator.getData(available_chunks_ids.get(idx_)));
        if(data != null) {
            data_ = new short[data.length];
            for (int i = 0; i < data.length; i++) {
                data_[i] = (short) (data[i] ^ key[currentPos++]);
                currentPos %= key.length;
            }
        } else {
            data_ = null;
        }
        synchronized (chunks) {
            chunks.put(idx_, data_);
        }
        consumer_notifier.notify(idx_);
        return RC.CODE_SUCCESS;
    }

    @Override
    public INotifier getNotifier() {
        return new XOR_Notifier();
    }

    private short[] convert(Object object_data){
        if (object_data == null) return null;
        short[] return_data = null;
        switch (producer_data_type){
            case SHORT:
                return_data = (short[]) object_data;
                break;
            case CHAR:
                return_data = new short[((char[])object_data).length];
                for (int i = 0; i < return_data.length; i++) {
                    return_data[i] = (short)((char[])object_data)[i];

                }
        }
        return return_data;
    }

    public void setKey(short[] key) {
        this.key = key;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return supported_types;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        return new XOR_Mediator(type);
    }

    @Override
    public void run() {
        while (data_!= null) {
            if(idx >= available_chunks_ids.size()) {
                synchronized (this) {
                    try {
                        this.wait(1000);
                    } catch (InterruptedException e) {
                        logger.warning(RC.CODE_SYNCHRONIZATION_ERROR.toString());
                        return;
                    }
                }
            }
            execute();
            synchronized (this) {
                idx++;
            }
        }
    }

    private class XOR_Mediator implements IMediator{
        private final TYPE type_;
        public XOR_Mediator(TYPE type){
            type_ = type;
        }
        @Override
        public Object getData(int idChunk) {
            Object return_data = null;
            synchronized (chunks) {
                data_ = chunks.get(idChunk);
                if (data_ == null) return null;
                switch (type_) {
                    case SHORT:
                        return_data = data_.clone();
                        break;
                    case CHAR:
                        return_data = new char[data_.length];
                        for (int i = 0; i < ((char[]) return_data).length; i++) {
                            ((char[]) return_data)[i] = (char) data_[i];
                        }
                        break;
                }
            }
            chunks.remove(idChunk);
            return return_data;
        }
    }

    private class XOR_Notifier implements INotifier{
        @Override
        public RC notify(int idChunk) {
            synchronized (XOR_Encoder.this) {
                XOR_Encoder.this.notify();
            }
            synchronized (available_chunks_ids) {
                if (available_chunks_ids.contains(idChunk))
                    return RC.CODE_WARNING_CHUNK_ALREADY_TAKEN;
                available_chunks_ids.add(idChunk);
            }
            return RC.CODE_SUCCESS;
        }
    }
}
