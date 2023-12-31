package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.*;

import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.security.data.SensorType;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;


import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.provider.ValueSource;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    private StatusListener statusListener;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository,imageService);
        sensor = new Sensor("DOOR_NAME", SensorType.DOOR);
    }

    private Set<Sensor> actionAddAllSensors(boolean checkAdd) {
        Set<Sensor> setSensor = new HashSet<>();
        Set<Sensor> allSensors = securityService.getSensors();
        if (allSensors.isEmpty()) {
            setSensor.add(new Sensor("MOTION", SensorType.MOTION));
            setSensor.add(new Sensor("WINDOW", SensorType.WINDOW));
            setSensor.add(new Sensor("DOOR", SensorType.DOOR));

            setSensor.stream().forEach(itemSensor -> {
                itemSensor.setActive(checkAdd);
            });
        }
        return setSensor;
    }

    @Test
    public void test_addStatusListener() {
        securityService.addStatusListener(statusListener);
    }

    @Test
    public void test_removeStatusListener() {
        securityService.removeStatusListener(statusListener);
    }

    @Test
    public void test_addSensor() {
        securityService.addSensor(sensor);
    }

    @Test
    public void test_removeSensor() {
        securityService.removeSensor(sensor);
    }

    //1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY"})
    public void test_alarmArmed_sensorActivated_alarmStatusAlarm(ArmingStatus alarmStatusAlarm) {
        when(securityRepository.getArmingStatus()).thenReturn(alarmStatusAlarm);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //2
    @Test
    public void test_alarmArmed_sensorActivated_StatusPending_alarmStatusAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //3
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void test_alarmPending_allSensorActivated_alarmStatusNoAlarm(boolean checkAdd) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Set<Sensor> setSensors = actionAddAllSensors(checkAdd);
        when(securityService.getSensors()).thenReturn(setSensors);

        securityService.changeSensorActivationStatus();

        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    //4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void test_alarmActive_changeSensorState_notAffectAlarmState(boolean changeStatusSensor) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, changeStatusSensor);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }


    //5
    @Test
    public void test_sensorActivatedWhileAlreadyActive_StatusPending_alarmStatusAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = { "NO_ALARM", "PENDING_ALARM", "ALARM" })
    public void test_sensorDeactivated_whileAlreadyInactive_noChangesAlarmState(AlarmStatus isAlarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(isAlarmStatus);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7
    @Test
    public void test_imageIdentifies_containingCat_systemArmedHome_alarmStatusAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8
    @Test
    public void test_imageIdentifies_notContainCat_sensorsInactive_alarmStatusNoAlarm() {
        Set<Sensor> addAllSensors = actionAddAllSensors(false);
        when(securityService.getSensors()).thenReturn(addAllSensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //9
    @Test
    public void test_systemDisarmed_alarmStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = { "ARMED_HOME", "ARMED_AWAY"})
    public void test_systemArmed_resetAllSensorsInactive(ArmingStatus alarmStatusAlarm) {
        Set<Sensor> addAllSensors = actionAddAllSensors(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityService.getSensors()).thenReturn(addAllSensors);
        securityService.setArmingStatus(alarmStatusAlarm);

        securityService.getSensors().stream().forEach(item -> {
            assertEquals(item.getActive(), false);
        });


    }

    //11
    @Test
    public void test_systemArmedHome_cameraShowsCat_alarmStatusAlarm() {
        BufferedImage catInformationImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(catInformationImage, 50.0f)).thenReturn(true);
        securityService.processImage(catInformationImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    //12
    @Test
    public void test_noProblem_systemDisarmed() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

}
