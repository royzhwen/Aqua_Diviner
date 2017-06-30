//
// Created by Owner on 6/2/2016.
//
#include <jni.h>

#ifndef WELLCALCULATOR_JNI_H
#define WELLCALCULATOR_JNI_H
#ifdef __cplusplus
extern "C" {
#endif //WELLCALCULATOR_JNI_H


JNIEXPORT jstring JNICALL  Java_com_example_owner_wellcalculator_MyActivity_getMsgFromJni
        (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif