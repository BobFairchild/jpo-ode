package us.dot.its.jpo.ode.plugin.j2735.oss;

import java.math.BigDecimal;

import us.dot.its.jpo.ode.j2735.dsrc.OffsetLL_B14;

public class OssOffsetLLB14 {

   private OssOffsetLLB14() {
      throw new UnsupportedOperationException();
   }

   public static OffsetLL_B14 offsetLLB14(BigDecimal offset) {
      return new OffsetLL_B14(offset.scaleByPowerOfTen(7).intValue());
   }

}
