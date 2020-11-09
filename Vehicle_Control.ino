#include <Adafruit_MotorShield.h>
#include <ArduinoBLE.h>

// Debugging switches and macros
#define DEBUG 1 // Switch debug output on and off by 1 or 0

#if DEBUG
#define PRINTS(s)   { Serial.print(F(s)); Serial.print("\n");}
#define PRINT(s,v)  { Serial.print(F(s)); Serial.print(v); Serial.print("\n");}
#define PRINTX(s,v) { Serial.print(F(s)); Serial.print(F("0x")); Serial.print(v, HEX);  Serial.print("\n");}
#else
#define PRINTS(s)
#define PRINT(s)
#define PRINT(s,v)
#define PRINTX(s,v)
#endif
// for saving the current motion
typedef struct{
  uint8_t orientation;
  String direction_;
  uint8_t speed_;
}VehicleMotion;

BLEService vehicleService("19B10000-E8F2-537E-4F6C-D104768A1214"); // BLE LED Service

// BLE LED Switch Characteristic - custom 128-bit UUID, read and writable by central
BLEStringCharacteristic vehicleMotionCharacteristic("19B10001-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite, 6);
BLEByteCharacteristic vehicleMotionStatusCharacteristic("19B10012-E8F2-537E-4F6C-D104768A1214", BLERead | BLENotify);
// init motors 
Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 
Adafruit_DCMotor *motorVL = AFMS.getMotor(1);
Adafruit_DCMotor *motorVR = AFMS.getMotor(2);
// set current motion with default vlaues 
VehicleMotion currentMotion = {FORWARD, "UNKOWN", 0};


void setup() {
  Serial.begin(9600);

  AFMS.begin();  // create with the default frequency 1.6KHz

  // begin initialization
  if (!BLE.begin()) {
    Serial.println("starting BLE failed!");

    while (1);
  }

  // set the local name peripheral advertises
  BLE.setLocalName("LEDCallback");
  // set the UUID for the service this peripheral advertises
  BLE.setAdvertisedService(vehicleService);
  // add the characteristic to the service
  vehicleService.addCharacteristic(vehicleMotionCharacteristic);
  vehicleService.addCharacteristic(vehicleMotionStatusCharacteristic);

  // add service
  BLE.addService(vehicleService);
  // assign event handlers for connected, disconnected to peripheral
  BLE.setEventHandler(BLEConnected, blePeripheralConnectHandler);
  BLE.setEventHandler(BLEDisconnected, blePeripheralDisconnectHandler);
  vehicleMotionCharacteristic.setEventHandler(BLEWritten, vehicleMotionCharacteristicWritten);
  vehicleMotionCharacteristic.setValue("");
  // start advertising
  BLE.advertise();
  Serial.println(("Bluetooth device active, waiting for connections..."));
}

void loop() {
    BLE.poll();
}

// ______________________________________________________

void blePeripheralConnectHandler(BLEDevice central) {
  // central connected event handler
  Serial.print("Connected event, central: ");
  Serial.println(central.address());
}

void blePeripheralDisconnectHandler(BLEDevice central) {
  // central disconnected event handler
  Serial.print("Disconnected event, central: ");
  Serial.println(central.address());
}
// called if there are some changes 
void vehicleMotionCharacteristicWritten(BLEDevice central, BLECharacteristic characteristic) {
  // central wrote new value to characteristic, update LED
  Serial.print("Characteristic event, written: ");
  String msg = characteristic.value();
  PRINT("Value: ", msg)
  char json[100];
  msg.toCharArray(json,100);
 // if received message has the right format, move vehicle 
  if(processStringCommand(json)){  
    moveVehicle(currentMotion);
  }
}

//______________________________________________________
boolean processStringCommand(char command[]){

  //char* command = strtok(input, ":");
  String first = String(command[0]);
  String secound = String(command[1]);
 
  PRINT("0: ", first);
  PRINT("1: ", secound);
  PRINT("size: ", sizeof(command));
  
  if(first == "F"){ // Forward
    PRINTS("Its F");
    currentMotion.orientation = FORWARD;
  }
  if(first == "B"){ // Backward
    currentMotion.orientation = BACKWARD;
  }
  if(first == "R"){ // Release
    currentMotion.orientation = RELEASE;
  }

  if(secound == "S"){ // STRAIGHT
    currentMotion.direction_ = "STRAIGHT";
  }
  if(secound == "L"){ // Left
    currentMotion.direction_ = "LEFT";
  }
  if(secound == "R"){ // Rigth
    currentMotion.direction_ = "RIGHT";
  }

  char buffer[4];
  buffer[0] = command[3];
  buffer[1] = command[4];
  buffer[2] = command[5];
  buffer[3] = '\0';

  int  speed_;
  speed_ = atoi(buffer);
  currentMotion.speed_ = speed_;
  return true; 
}

// ______________________________________________________
void moveVehicle(VehicleMotion vMotion) {
  PRINTS("Start move vehicle");
  uint8_t leftSpeed = vMotion.speed_;
  uint8_t rightSpeed = vMotion.speed_;

  if(vMotion.direction_ == "LEFT"){
 //if(strcmp(charArray,"LEFT")==0){
    leftSpeed = (uint8_t) (rightSpeed * 6/10);
  }
  if(vMotion.direction_ == "RIGHT"){
  //if(strcmp(charArray,"RIGHT")==0){
    rightSpeed =  (uint8_t) (leftSpeed * 6/10);
  }
  PRINT("Left speed: ", leftSpeed);
  PRINT("right speed: ", rightSpeed);
  PRINT("Orientation: ", vMotion.orientation);


  motorVR->setSpeed(rightSpeed);
  //motorHR->setSpeed(rightSpeed);
  motorVL->setSpeed(leftSpeed);
  //motorHL->setSpeed(leftSpeed);

  motorVR->run(vMotion.orientation);
  motorVL->run(vMotion.orientation);
}
