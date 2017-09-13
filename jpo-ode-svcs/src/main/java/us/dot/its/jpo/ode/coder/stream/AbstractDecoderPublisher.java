package us.dot.its.jpo.ode.coder.stream;

import java.io.BufferedInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dot.its.jpo.ode.coder.BsmDecoderHelper;
import us.dot.its.jpo.ode.coder.MessagePublisher;
import us.dot.its.jpo.ode.importer.BsmFileParser;
import us.dot.its.jpo.ode.importer.TimFileParser;
import us.dot.its.jpo.ode.model.SerialId;

public abstract class AbstractDecoderPublisher implements DecoderPublisher {

   protected static final Logger logger = LoggerFactory.getLogger(AbstractDecoderPublisher.class);

   protected SerialId serialId;

   protected MessagePublisher publisher;

   protected BsmDecoderHelper bsmDecoder;
   protected BsmFileParser bsmFileParser;
   protected TimFileParser timFileParser;

   protected static AtomicInteger bundleId = new AtomicInteger(1);

   public AbstractDecoderPublisher(
       MessagePublisher dataPub) {
      this.publisher = dataPub;

      this.serialId = new SerialId();
      this.serialId.setBundleId(bundleId.incrementAndGet());
      this.bsmDecoder = new BsmDecoderHelper();
   }

   @Override
   public void decodeAndPublish(BufferedInputStream is, String fileName, boolean hasMetadataHeader) throws Exception {
       /* 
        * CAUTION: bsmFileParser needs to be created here and should not be moved to the
        * constructor because only one DecoderPublisher exists per filetype/upload directory
        * and we need to have a new BsmFileParser per uploaded file. If we put this instantiation
        * in the constructor, all uploaded files will be using the same parser which may throw 
        * off the parsing.
        */
       this.bsmFileParser = new BsmFileParser();
       this.timFileParser = new TimFileParser();
   }
}
