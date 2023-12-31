package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.Sensor;


import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();

    private boolean catCheckDetect = false;
    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            Set<Sensor> setSensors = Collections.synchronizedSet(new HashSet<>(getSensors()));
            setSensors.forEach(item -> changeSensorActivationStatus(item ,false));
            if(ArmingStatus.ARMED_HOME.equals(armingStatus) && catCheckDetect){
                setAlarmStatus(AlarmStatus.ALARM);
            }
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        if(getArmingStatus() == ArmingStatus.ARMED_HOME && cat) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
        if(!cat && isActiveAllSensor())
        {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(item -> item.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
//            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }
    private boolean isActiveAllSensor() {
        for(Sensor itemSensor: getSensors()) {
            if(itemSensor.getActive()) {
                return false;
            }
        }
        return true;
    }
    public void changeSensorActivationStatus() {
        if (AlarmStatus.PENDING_ALARM.equals(getAlarmStatus()) && isActiveAllSensor()) {
            handleSensorDeactivated();
        }
        getSensors().forEach(itemSensor -> securityRepository.updateSensor(itemSensor));
    }
    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if(!AlarmStatus.ALARM.equals(getAlarmStatus())) {
            if (sensor.getActive()) {
                handleSensorDeactivated();
            }
            if( active) {
                handleSensorActivated();
            }
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }



    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catCheckDetect = imageService.imageContainsCat(currentCameraImage, 50.0f);
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

}
