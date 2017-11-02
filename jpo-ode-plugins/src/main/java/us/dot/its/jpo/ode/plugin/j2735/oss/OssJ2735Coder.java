package us.dot.its.jpo.ode.plugin.j2735.oss;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oss.asn1.AbstractData;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;
import com.oss.asn1.PERUnalignedCoder;

import us.dot.its.jpo.ode.j2735.J2735;
import us.dot.its.jpo.ode.j2735.dsrc.BasicSafetyMessage;
import us.dot.its.jpo.ode.j2735.dsrc.MessageFrame;
import us.dot.its.jpo.ode.plugin.OdePlugin;
import us.dot.its.jpo.ode.plugin.asn1.Asn1Object;
import us.dot.its.jpo.ode.plugin.asn1.J2735Plugin;
import us.dot.its.jpo.ode.plugin.j2735.J2735Bsm;
import us.dot.its.jpo.ode.plugin.j2735.J2735MessageFrame;
import us.dot.its.jpo.ode.plugin.j2735.oss.OssBsmPart2Content.OssBsmPart2Exception;
import us.dot.its.jpo.ode.plugin.j2735.oss.OssMessageFrame.OssMessageFrameException;

public class OssJ2735Coder implements J2735Plugin {

   private static Logger logger = LoggerFactory.getLogger(OssJ2735Coder.class);

   private PERUnalignedCoder coder;

   public OssJ2735Coder() {
      coder = J2735.getPERUnalignedCoder();
   }

   @Override
   public Asn1Object decodeUPERMessageFrameHex(String hexMsg) {
      return decodeUPERMessageFrameBytes(DatatypeConverter.parseHexBinary(hexMsg));
   }

   @Override
   public Asn1Object decodeUPERMessageFrameBytes(byte[] byteArrayMsg) {

      InputStream ins = new ByteArrayInputStream(byteArrayMsg);

      MessageFrame mf = new MessageFrame();

      J2735MessageFrame returnValue = null;

      try {
         coder.decode(ins, mf);
         returnValue = OssMessageFrame.genericMessageFrame(mf);
      } catch (DecodeFailedException e) {
         AbstractData partialDecodedMessage = e.getDecodedData();
         if (partialDecodedMessage != null) {
            logger.error("Error, message only partially decoded: {}", partialDecodedMessage);
            try {
               returnValue = OssMessageFrame.genericMessageFrame((MessageFrame) partialDecodedMessage);
            } catch (OssMessageFrameException | OssBsmPart2Exception e1) {
               logger.debug("Failed to translate partially decoded message.", e1);
            }
         } else {
            logger.debug("Ignoring extraneous bytes at the end of the input stream.");
         }
      } catch (OssMessageFrameException | OssBsmPart2Exception | DecodeNotSupportedException e) {
         logger.debug("Failed to decode message frame.", e);
      } finally {
         try {
            ins.close();
         } catch (IOException e) {
            logger.warn("Error closing input stream: ", e);
         }
      }

      return returnValue;
   }

   @Override
   public Asn1Object decodeUPERBsmBytes(byte[] byteArrayMsg) {
      InputStream ins = new ByteArrayInputStream(byteArrayMsg);

      BasicSafetyMessage bsm = new BasicSafetyMessage();
      J2735Bsm gbsm = null;

      try {
         coder.decode(ins, bsm);
         gbsm = OssBsm.genericBsm(bsm);
      } catch (DecodeFailedException e) {
         AbstractData partialDecodedMessage = e.getDecodedData();
         if (partialDecodedMessage != null) {
            logger.error("Error, message only partially decoded: {}", partialDecodedMessage);
            try {
               gbsm = OssBsm.genericBsm((BasicSafetyMessage) partialDecodedMessage);
            } catch (OssBsmPart2Exception e1) {
               logger.debug("Failed to translate partially decoded message.", e1);
            }
         } else {
            logger.debug("Ignoring extraneous bytes at the end of the input stream.");
         }
      } catch (DecodeNotSupportedException | OssBsmPart2Exception e) {
         logger.error("Failed to decode BSM byte array.", e);

      } finally {
         try {
            ins.close();
         } catch (IOException e) {
            logger.warn("Error closing input stream: ", e);
         }
      }

      return gbsm;
   }

   @Override
   public Asn1Object decodeUPERBsmStream(BufferedInputStream bis) {
      J2735Bsm gbsm = null;

      try {
         if (bis.available() > 0) {
            // This is the end of the line. No more resetting the input stream
            // if (bis.markSupported()) {
            // bis.mark(OdePlugin.INPUT_STREAM_BUFFER_SIZE);
            // }
            BasicSafetyMessage bsm = new BasicSafetyMessage();
            coder.decode(bis, bsm);
            gbsm = OssBsm.genericBsm(bsm);
         }
      } catch (Exception e) {
         // This is the end of the line. No more resetting the input stream
         // if (bis.markSupported()) {
         // try {
         // bis.reset();
         // } catch (IOException ioe) {
         // logger.error("Error reseting Input Stream to marked position", ioe);
         // handleDecodeException(ioe);
         // }
         // }
         handleDecodeException(e);
      }

      return gbsm;
   }

   @Override
   public Asn1Object decodeUPERMessageFrameStream(BufferedInputStream bis) {
      MessageFrame mf = new MessageFrame();
      J2735MessageFrame gmf = null;

      try {
         if (bis.available() > 0) {
            if (bis.markSupported()) {
               bis.mark(OdePlugin.INPUT_STREAM_BUFFER_SIZE);
            } else {
               logger.debug("Mark not supported.");
            }
            coder.decode(bis, mf);
            gmf = OssMessageFrame.genericMessageFrame(mf);
         } else {
            logger.debug("No bytes available.");
         }
      } catch (Exception e) {
         if (bis.markSupported()) {
            try {
               bis.reset();
            } catch (IOException ioe) {
               logger.error("Error reseting Input Stream to marked position", ioe);
               handleDecodeException(ioe);
            }
         }
         handleDecodeException(e);
      }

      return gmf;
   }

   public void handleDecodeException(Exception e) {

      if (DecodeFailedException.class == e.getClass()) {
         AbstractData partialDecodedMessage = ((DecodeFailedException) e).getDecodedData();
         if (partialDecodedMessage != null) {
            logger.error("Error, message only partially decoded: {}", partialDecodedMessage);
         } else {
            logger.debug("Ignoring extraneous bytes at the end of the input stream.");
         }
      } else if (DecodeNotSupportedException.class == e.getClass()) {
         logger.error("Error decoding, data does not represent valid message", e);
      } else if (IOException.class == e.getClass()) {
         logger.error("Error decoding", e);
      } else if (OssBsmPart2Exception.class == e.getClass()) {
         logger.error("Error decoding, BSM part 2 exception", e);
      } else if (OssMessageFrameException.class == e.getClass()) {
         logger.error("Error decoding, message frame exception", e);
      } else {
         logger.error("Unknown error", e);
      }
   }

   @Override
   public String encodeUPERBase64(Asn1Object asn1Object) {
      return DatatypeConverter.printBase64Binary(encodeUPERBytes(asn1Object));
   }

   @Override
   public String encodeUPERHex(Asn1Object asn1Object) {
      return DatatypeConverter.printHexBinary(encodeUPERBytes(asn1Object));
   }

   @Override
   public byte[] encodeUPERBytes(Asn1Object asn1Object) {
      if (asn1Object instanceof J2735Bsm) {
         J2735Bsm genericBsm = (J2735Bsm) asn1Object;
         try {
            ByteBuffer bytes = coder.encode(OssBsm.basicSafetyMessage(genericBsm));
            return bytes.array();
         } catch (Exception e) {
            logger.warn("Error encoding data.", e);
         }
      }

      return new byte[0];
   }

   @Override
   public Asn1Object decodeUPERBsmHex(String hexMsg) {
      return decodeUPERBsmBytes(DatatypeConverter.parseHexBinary(hexMsg));
   }

}
