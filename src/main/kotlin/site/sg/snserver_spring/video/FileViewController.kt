package site.sg.snserver_spring.video

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.util.FileCopyUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RestController
class FileViewController {

    private final val DURATION_SEGMENT_SECONDS = 10 * 1000000

    @GetMapping("/video", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getVideoFileBytes(): ByteArrayResource {
        val resource = ClassLoader.getSystemResource("data/test1.mp4") // Use ClassLoader directly
        return ByteArrayResource(FileCopyUtils.copyToByteArray(resource.openStream()))
    }

    @PostMapping("/video")
    fun uploadVideoFile(@RequestParam("file") uploadedFile: MultipartFile) {
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        val filename = uploadedFile.originalFilename ?: "temp-video.mp4"
        val filenameNoExt = uploadedFile.originalFilename?.substringBeforeLast(".") ?: "temp-video"
        val file = File(ClassLoader.getSystemResource("data/output").path, filename)
        uploadedFile.transferTo(file)

        splitVideo(file.absolutePath, filenameNoExt, executor)
    }

    private fun splitVideo(filePath: String, filenameNoExt: String, executor: ExecutorService) {
        try {
            FFmpegFrameGrabber(filePath).use { grabber ->
                grabber.start()

                val totalDuration = grabber.lengthInTime
                val segmentLength = totalDuration / DURATION_SEGMENT_SECONDS

                val tempFolder = File(ClassLoader.getSystemResource("data/output").path, filenameNoExt)
                if (!tempFolder.exists()) {
                    tempFolder.mkdir()
                }

                // write m3u8 file
                val m3u8File = File(tempFolder, "index.m3u8")
                m3u8File.writeText("#EXTM3U")
                m3u8File.appendText("\n#EXT-X-VERSION:3")
                m3u8File.appendText("\n#EXT-X-TARGETDURATION:2")
                m3u8File.appendText("\n#EXT-X-MEDIA-SEQUENCE:10")
                m3u8File.appendText("\n#EXT-FIRST-SEGMENT-TIMESTAMP:0")

                for (segment in 0..segmentLength) {
                    val startTimestamp = segment * DURATION_SEGMENT_SECONDS
                    val endTimestamp =
                        if (segment == segmentLength) totalDuration else (segment + 1) * DURATION_SEGMENT_SECONDS

                    val outputFileName = "segment_$segment.ts"
                    val outputFilePath = File(tempFolder, outputFileName).absolutePath

                    executor.submit {
                        try {
                            processSegment(filePath, outputFilePath, startTimestamp, endTimestamp)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }

                    m3u8File.appendText("\n#EXTINF:10.00")
                    m3u8File.appendText("\nsegment_$segment.ts")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    private fun processSegment(
        inputFilePath: String,
        outputFilePath: String,
        startTimestamp: Long,
        endTimestamp: Long
    ) {

        try {
            FFmpegFrameGrabber(inputFilePath).use { grabber ->
                grabber.start()
                grabber.timestamp = startTimestamp
                FFmpegFrameRecorder(outputFilePath, grabber.imageWidth, grabber.imageHeight).use { recorder ->
                    configureRecorder(recorder, grabber)
                    recorder.start()

                    var frame: Frame?
                    while ((grabber.grabFrame().also { frame = it }) != null && grabber.timestamp < endTimestamp) {
                        recorder.record(frame)
                    }

                    recorder.stop()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configureRecorder(recorder: FFmpegFrameRecorder, grabber: FFmpegFrameGrabber) {
        recorder.format = "ts"
        recorder.frameRate = grabber.frameRate
        recorder.videoBitrate = grabber.videoBitrate
        recorder.videoCodec = avcodec.AV_CODEC_ID_H264
        recorder.pixelFormat = avutil.AV_PIX_FMT_YUV420P
        recorder.audioChannels = grabber.audioChannels
        recorder.sampleRate = grabber.sampleRate
        recorder.audioBitrate = grabber.audioBitrate
        recorder.audioCodec = grabber.audioCodec
    }

}