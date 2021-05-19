/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.media.MediaCodecInfo;
import androidx.annotation.Nullable;
import java.util.Arrays;

/** Factory for Android platform software VideoDecoders. */
public class PlatformSoftwareVideoDecoderFactory extends MediaCodecVideoDecoderFactory {
  /**
   * Default allowed predicate.
   */
  private static final Predicate<MediaCodecInfo> defaultAllowedPredicate =
      new Predicate<MediaCodecInfo>() {
        @Override
        public boolean test(MediaCodecInfo arg) {
          if(!MediaCodecUtils.isSoftwareOnly(arg)) {
              return false;
          }

          String[] types = arg.getSupportedTypes();
          if (types == null || types.length == 0) {
              return false;
          }

          for (int type = 0; type < types.length; type++) {
              switch (types[type]) {
                  case "video/avc":
                      return false; // H264 platform software decoder have issues on StarGate.
                  case "video/hevc":
                      return false; // H265 platform software decoder have issues on StarGate.
              }
          }
          return true;
        }
      };

  /**
   * Creates a PlatformSoftwareVideoDecoderFactory that supports surface texture rendering.
   *
   * @param sharedContext The textures generated will be accessible from this context. May be null,
   *                      this disables texture support.
   */
  public PlatformSoftwareVideoDecoderFactory(@Nullable EglBase.Context sharedContext) {
    super(sharedContext, defaultAllowedPredicate);
  }
}
