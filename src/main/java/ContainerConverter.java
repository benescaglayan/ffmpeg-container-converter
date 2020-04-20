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
        AVFormatContext inputFormatContext = new AVFormatContext(null); // allocates space to hold information about the input file's container

        if (avformat_open_input(inputFormatContext, inputFilename, null, null) < 0) { // terminates if unable to put information about input file's container into inputFormatContext
            System.out.println("Could not open the input file.");
            System.exit(-1);
        }

        if (avformat_find_stream_info(inputFormatContext, (AVDictionary) null) < 0) {  // terminates if there is no stream in input container
            System.out.println("Could not find any information for streams.");
            System.exit(-1);
        }

        AVFormatContext outputFormatContext = avformat_alloc_context(); // allocates space to hold information about the output file's container

        if (avformat_alloc_output_context2(outputFormatContext,null, null, outputFilename) < 0) { // terminates if unable to create output file's container with specified format
            System.out.println("Invalid output format.");
            System.exit(-1);
        }

        for (int i = 0; i < inputFormatContext.nb_streams(); i++) { // copies stream content from input file's container to output file's container
            AVStream inputStream = inputFormatContext.streams(i);
            AVStream outputStream = avformat_new_stream(outputFormatContext, null); // allocates space to hold stream info and binds it to outputFormatContext

            if (avcodec_parameters_copy(outputStream.codecpar(), inputStream.codecpar()) < 0) { // terminates if unable to copy codecs from input file's stream to output file's stream
                System.out.println("Could not transfer the stream" + i);
                System.exit(-1);
            }

            outputStream.codecpar().codec_tag(0); // lets the muxer handle setting of codec tag
            outputStream.time_base(inputStream.time_base()); // sets the timebase
        }

        if (!(outputFormatContext.oformat().flags() == 1 && AVFMT_NOFILE == 1)) {
            AVIOContext pb = new AVIOContext(null); // allocates space buffering
            if (avio_open(pb, outputFilename, AVIO_FLAG_WRITE) < 0) { // terminates if unable to create output file with the buffer
                System.out.println("Could not create the output file.");
                System.exit(-1);
            }
            outputFormatContext.pb(pb); // sets the buffer of outputFormatContext to created file's buffer
        }

        if (avformat_write_header(outputFormatContext, (AVDictionary) null) < 0) { // terminates if unable to write header of the output file' container
            System.out.println("Could not write the header.");
            System.exit(-1);
        }

        AVPacket packet = new AVPacket(); // allocates space to hold temporary packets
        while (!(av_read_frame(inputFormatContext, packet) < 0)) { // stops looping if unable to read anymore packets from inputFormatContext
            if (av_write_frame(outputFormatContext, packet) < 0) { // skips if unable to write packet into outputFormatContext
                System.out.println("Could not write frame. stream: " + packet.stream_index() +
                        ", pts: " + packet.pts() * inputFormatContext.streams(packet.stream_index()).time_base().num());
            }

            av_packet_unref(packet); // clears the packet variable
        }

        avformat_close_input(inputFormatContext); // deallocates space for input file's container

        av_write_trailer(outputFormatContext); // writes the trailer data of output stream

        avformat_free_context(outputFormatContext); // deallocates space for output file's container
    }

}
