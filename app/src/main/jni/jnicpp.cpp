#include "jnicpp.h";
//#include <cstdio>
 #include <cstring>
 #include <cstdlib>
#include <ctime>
 #include <cmath>
/*

double rngtest_uniform(){
	srand(time(NULL));
 	double mean = 0;
 	double variance = 0;
 	for (int i = 0; i < 1000; i++) {
 		double rng = (double)rand() / (RAND_MAX);
 		mean += rng;
 		variance += pow((rng - 0.5), 2);
 	}
 	mean = mean / 1000;
 	variance = variance / 1000;
 	return mean+variance;
 }

*/

 JNIEXPORT jstring JNICALL
 Java_com_example_owner_wellcalculator_MyActivity_getMsgFromJni(JNIEnv *env, jobject instance) {
	 //return rngtest_uniform();
	 //return to_string(123.123);
    return (*env).NewStringUTF("Sdasda"); //For String returns
 }