package org.thankthemaker.quarkus.internal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

    final class UnsafeAccessSubstitution {}

    @TargetClass(className = "com.lmax.disruptor.RingBufferFields")
    final class Target_com_lmax_disruptor_RingBufferFields {
        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FieldOffset, name = "REF_ARRAY_BASE")
        private static long REF_ARRAY_BASE;

        @Alias
        @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexShift, declClass = Object[].class, name = "REF_ELEMENT_SHIFT")
        public static int REF_ELEMENT_SHIFT;
    }