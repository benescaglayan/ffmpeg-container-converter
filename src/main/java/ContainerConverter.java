import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

public class ContainerConverter {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please specify the input filename and output filename along with their extensions.\nExample usage: sample.flv converted.mp4");
            System.exit(-1);
        }

        convertContainer(args[0], args[1]);
    }

    public static void convertContainer(String inputFilename, String outputFilename) {
        AVFormatContext inputFormatContext = new AVFormatContext(null);

        if (avformat_open_input(inputFormatContext, inputFilename, null, null) < 0) {
            System.out.println("Could not open the input file.");
            System.exit(-1);
        }

        if (avformat_find_stream_info(inputFormatContext, (AVDictionary) null) < 0) {
            System.out.println("Could not find any information for streams.");
            System.exit(-1);
        }

        AVFormatContext outputFormatContext = avformat_alloc_context();

        if (avformat_alloc_output_context2(outputFormatContext,null, null, outputFilename) < 0) {
            System.out.println("Invalid output format.");
            System.exit(-1);
        }

        for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
            AVStream inputStream = inputFormatContext.streams(i);
            AVStream outputStream = avformat_new_stream(outputFormatContext, null);

            if (avcodec_parameters_copy(outputStream.codecpar(), inputStream.codecpar()) < 0) {
                System.out.println("Could not transfer the stream" + i);
                System.exit(-1);
            }

            outputStream.codecpar().codec_tag(0);
            outputStream.time_base(inputStream.time_base());
        }

        if (!(outputFormatContext.oformat().flags() == 1 && AVFMT_NOFILE == 1)) {
            AVIOContext pb = new AVIOContext(null);
            if (avio_open(pb, outputFilename, AVIO_FLAG_WRITE) < 0) {
                System.out.println("Could not create the output file.");
                System.exit(-1);
            }
            outputFormatContext.pb(pb);
        }

        if (avformat_write_header(outputFormatContext, (AVDictionary) null) < 0) {
            System.out.println("Could not write the header.");
            System.exit(-1);
        }

        AVPacket packet = new AVPacket();
        while (!(av_read_frame(inputFormatContext, packet) < 0)) {
            if (av_write_frame(outputFormatContext, packet) < 0) {
                System.out.println("Could not write frame. stream: " + packet.stream_index() +
                        ", pts: " + packet.pts() * inputFormatContext.streams(packet.stream_index()).time_base().num());
            }

            av_packet_unref(packet);
        }

        avformat_close_input(inputFormatContext);

        av_write_trailer(outputFormatContext);

        avformat_free_context(outputFormatContext);
    }

}
