package xyz.sl.coderemote.core.vfs.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import xyz.sl.coderemote.core.vfs.Resource;

/**
 * 自动读写
 */
public class ResourceIO {
    private final Map<String, Class<?>> mimeMapping = new HashMap<>();

    public ResourceIO() {
//        register(String.class, new TextProcessor());
//        register(Bitmap.class, new BitmapProcessor());
        // 注册 MIME 映射
//        mimeMapping.put("text/plain", String.class);
//        mimeMapping.put("image/png", Bitmap.class);
    }

    private final Map<Class<?>, ResourceReader<?>> readers = new HashMap<>();
    private final Map<Class<?>, ResourceWriter<?>> writers = new HashMap<>();

    public <T> T read(Resource resource, Class<T> type) throws IOException {
        @SuppressWarnings("unchecked")
        ResourceReader<T> reader = (ResourceReader<T>) readers.get(type);
        if (reader == null) throw new UnsupportedOperationException("No reader for " + type);
        return reader.read(resource);
    }

    public <T> void write(Resource resource, T data) throws IOException {
        Class<?> type = data.getClass();
        @SuppressWarnings("unchecked")
        ResourceWriter<T> writer = (ResourceWriter<T>) writers.get(type);
        if (writer == null) throw new UnsupportedOperationException("No writer for " + type);
        writer.write(resource, data);
    }

    // 自动根据 Resource 的 MIME 类型打开（扩展功能）
    public Object readAuto(Resource resource) throws IOException {
//        String mime = resource.getMimeType(); // 需提前在 Resource 中加此方法
//        Class<?> targetType = mimeMapping.get(mime);
//        if (targetType == null) targetType = byte[].class;
//        return read(resource, targetType);
        return null;
    }
}
