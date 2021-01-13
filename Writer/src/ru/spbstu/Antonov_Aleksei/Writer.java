package ru.spbstu.Antonov_Aleksei;

import ru.spbstu.pipeline.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Writer implements IWriter {
    private FileOutputStream outputStream;
    private final Logger logger;
    private final TYPE[] supported_types = new TYPE[]{TYPE.BYTE, TYPE.SHORT};
    private byte[] byteBuffer;
    private IMediator producerMediator;
    private TYPE producer_data_type;

    private final ArrayList<Integer> available_chunks_ids = new ArrayList<>();

    public Writer(Logger logger){
        this.logger = logger;
    }

    @Override
    public RC setConfig(String cfg_src) {
        ConfigParser parser = new ConfigParser(logger);
        Map<Param, String> params = parser.create_object(cfg_src, new WriterParams());
        if(params == null){
            return RC.CODE_CONFIG_GRAMMAR_ERROR;
        }
        for (Map.Entry<Param, String> param_value : params.entrySet()){
            Param param = param_value.getKey();
            if(param instanceof WriterParams.Params){
                param.setValue(this, param_value.getValue());
            } else {
                return RC.CODE_INVALID_ARGUMENT;
            }
        }
        return RC.CODE_SUCCESS;
    }
    @Override
    public RC addNotifier(INotifier iNotifier) {
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
        int cur_chunk = 0;
        while (true){
            synchronized (available_chunks_ids) {
                if (!available_chunks_ids.contains(cur_chunk)) {
                    synchronized (this) {
                        try {
                            wait(1000);
                        } catch (InterruptedException e) {
                            logger.warning(RC.CODE_SYNCHRONIZATION_ERROR.toString());
                            return RC.CODE_SYNCHRONIZATION_ERROR;
                        }
                    }
                    continue;
                }
            }
            Object data_object = producerMediator.getData(cur_chunk++);
            if(data_object == null){
                break;
            }
            byte[] data = convert(data_object);
            int start = 0;

            while (start < data.length){
                System.arraycopy(data, start, byteBuffer, 0, Math.min(byteBuffer.length, data.length - start));
                try {
                    outputStream.write(byteBuffer, 0, Math.min(byteBuffer.length, data.length - start));
                } catch (IOException e) {
                    logger.warning(RC.CODE_FAILED_TO_WRITE.toString());
                    return RC.CODE_FAILED_TO_WRITE;
                }
                start += byteBuffer.length;
            }
        }
        return RC.CODE_SUCCESS;
    }

    @Override
    public INotifier getNotifier() {
        return new WriterNotifier();
    }

    byte[] convert(Object data){
        byte[] return_data = null;
        switch (producer_data_type){
            case SHORT:
                short[] short_data = (short[]) data;
                return_data = new byte[short_data.length * 2];
                for (int i = 0; i < short_data.length; i++) {
                    return_data[i * 2 + 1] = (byte) (short_data[i] & 0xff);
                    return_data[i * 2] = (byte) ((short_data[i] >> 8) & 0xff);
                }
                break;
            case BYTE:
                return_data = (byte[]) data;
        }
        return return_data;
    }

    @Override
    public RC setOutputStream(FileOutputStream fos) {
        outputStream = fos;
        return RC.CODE_SUCCESS;
    }

    void setBuffer(byte[] buf){
        byteBuffer = buf;
    }

    @Override
    public void run() {
        execute();
    }

    private class WriterNotifier implements INotifier{
        @Override
        public RC notify(int idChunk) {
            synchronized (Writer.this) {
                Writer.this.notify();
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
