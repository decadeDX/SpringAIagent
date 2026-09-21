package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.config.KnowledgeProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 管理知识源文件的安全本地读写，所有数据库路径均相对于受控根目录保存。
 */
@Component
public class KnowledgeFileStorage {

    /** 知识文件目录与缓存相关运行配置。 */
    private final KnowledgeProperties knowledgeProperties;

    /**
     * 创建知识源文件存储服务。
     *
     * @param knowledgeProperties 本地存储根目录配置
     */
    public KnowledgeFileStorage(KnowledgeProperties knowledgeProperties) {
        this.knowledgeProperties = knowledgeProperties;
    }

    /**
     * 严格按 UTF-8 解码上传字节，拒绝包含替换字符的损坏文本。
     *
     * @param content 上传的原始字节
     * @return 已验证的 UTF-8 文本
     * @throws CharacterCodingException 字节不符合 UTF-8 时抛出
     */
    public String decodeUtf8(byte[] content) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content))
                .toString();
    }

    /**
     * 将源文件写入以文档版本主键命名的受控目录，并返回数据库使用的相对路径。
     *
     * @param documentId 文档版本主键
     * @param extension 已校验的小写扩展名
     * @param content 已验证的 UTF-8 原始字节
     * @return 使用正斜杠表示的相对路径
     * @throws IOException 目录或文件写入失败时抛出
     */
    public String store(Long documentId, String extension, byte[] content) throws IOException {
        String relativePath = documentId + "/source." + extension;
        Path target = resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return relativePath;
    }

    /**
     * 读取数据库中保存的相对路径对应的 UTF-8 文本。
     *
     * @param relativePath 数据库存储的受控相对路径
     * @return 完整源文件文本
     * @throws IOException 文件缺失或编码无效时抛出
     */
    public String readUtf8(String relativePath) throws IOException {
        try {
            return decodeUtf8(Files.readAllBytes(resolve(relativePath)));
        } catch (CharacterCodingException exception) {
            throw new IOException("知识源文件不是有效 UTF-8", exception);
        }
    }

    /**
     * 将相对路径解析至存储根目录内，阻止数据库异常数据穿越到目录外。
     *
     * @param relativePath 受控相对路径
     * @return 规范化后的绝对目标路径
     */
    private Path resolve(String relativePath) {
        Path root = knowledgeProperties.storagePath().toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("知识源文件路径非法");
        }
        return target;
    }
}
