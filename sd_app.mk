# Copyright (C) 2020 SberDevices Inc
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

TELEGRAM_RELATIVE_PATH := android

include $(SD_APP_CLEAR_VARS)

SD_APP := TelegramCalls
TELEGRAM_GRADLE_APK_PATH := $(STAROS_APKS_BUILD_DIR)/$(TELEGRAM_RELATIVE_PATH)/TMessagesProj/$(SD_APP)_build/outputs/apk/debug/app.apk

SD_APP_OUT_OF_TREE_DIR := $(STAROS_APKS_BUILD_DIR)
SD_APP_ADDITIONAL_PATH := $(TELEGRAM_RELATIVE_PATH)
SD_APP_BUILD_ARTEFACT_PATH := $(TELEGRAM_GRADLE_APK_PATH)

SD_APP_APK_BUILD_TYPE := Debug
SD_APP_PRIVELEGED := true
SD_APP_VENDOR_MODULE := true
SD_APP_GENERAL_GRADLE_PROJECT := true

include $(BUILD_SD_APP)
