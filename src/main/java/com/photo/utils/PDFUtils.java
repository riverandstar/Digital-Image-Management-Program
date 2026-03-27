package com.photo.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * PDF转换工具类（图片转PDF）
 */
public class PDFUtils {

    /**
     * 将多张图片转换为单个PDF文件
     * @param imageFiles 图片文件列表（按顺序排列）
     * @param outputPdfFile 输出的PDF文件路径
     * @throws IOException 转换异常
     */
    public static void imagesToPdf(List<File> imageFiles, File outputPdfFile) throws IOException {
        // 校验参数
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("图片文件列表不能为空");
        }
        if (outputPdfFile == null) {
            throw new IllegalArgumentException("PDF输出文件不能为空");
        }

        // 创建PDF文档
        try (PDDocument document = new PDDocument()) {
            for (File imageFile : imageFiles) {
                // 校验图片文件
                if (!imageFile.exists() || !FileUtils.isImageFile(imageFile)) {
                    throw new IOException("无效的图片文件：" + imageFile.getAbsolutePath());
                }

                // 读取图片为BufferedImage
                BufferedImage bufferedImage = ImageIO.read(imageFile);
                if (bufferedImage == null) {
                    throw new IOException("无法读取图片：" + imageFile.getAbsolutePath());
                }

                // 创建PDF页面（适配图片尺寸）
                PDRectangle pageSize = new PDRectangle(bufferedImage.getWidth(), bufferedImage.getHeight());
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                // 将图片写入PDF页面
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    PDImageXObject pdImage = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), document);
                    // 绘制图片（填满整个页面）
                    contentStream.drawImage(pdImage, 0, 0, pageSize.getWidth(), pageSize.getHeight());
                }
            }

            // 保存PDF文件
            document.save(outputPdfFile);
        }
    }
}