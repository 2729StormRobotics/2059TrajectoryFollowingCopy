// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.kauailabs.navx.frc.AHRS;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMax.IdleMode;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.DifferentialDriveOdometry;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.interfaces.Gyro;
import edu.wpi.first.wpilibj.motorcontrol.MotorControllerGroup;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.DriveTrainConstants;

public class DrivetrainSubsystem extends SubsystemBase {

  CANSparkMax leftFrontMotor = new CANSparkMax(Constants.DriveTrainConstants.leftFrontCANID,
      CANSparkMaxLowLevel.MotorType.kBrushless);
  CANSparkMax leftBackMotor = new CANSparkMax(Constants.DriveTrainConstants.leftBackCANID,
      CANSparkMaxLowLevel.MotorType.kBrushless);
  CANSparkMax rightFrontMotor = new CANSparkMax(Constants.DriveTrainConstants.rightFrontCANID,
      CANSparkMaxLowLevel.MotorType.kBrushless);
  CANSparkMax rightBackMotor = new CANSparkMax(Constants.DriveTrainConstants.rightBackCANID,
      CANSparkMaxLowLevel.MotorType.kBrushless);

  RelativeEncoder leftEncoder = leftFrontMotor.getEncoder();
  RelativeEncoder rightEncoder = rightFrontMotor.getEncoder();

  // MotorControllerGroup leftControllerGroup = new MotorControllerGroup(leftFrontMotor, leftBackMotor);
  // MotorControllerGroup rightControllerGroup = new MotorControllerGroup(rightFrontMotor, rightBackMotor);

  DifferentialDrive differentialDrive = new DifferentialDrive(leftFrontMotor, rightFrontMotor);

  public Field2d m_field = new Field2d();
  public AHRS ahrs;
  private final DifferentialDriveOdometry m_odometry;

  /** Creates a new ExampleSubsystem. */
  public DrivetrainSubsystem() {
    leftFrontMotor.restoreFactoryDefaults();
    leftBackMotor.restoreFactoryDefaults();
    rightFrontMotor.restoreFactoryDefaults();
    rightBackMotor.restoreFactoryDefaults();
    
    leftFrontMotor.setInverted(true);

    leftEncoder.setPosition(0);
    rightEncoder.setPosition(0);

    rightEncoder.setPositionConversionFactor(DriveTrainConstants.kLinearDistanceConversionFactor);
    leftEncoder.setPositionConversionFactor(DriveTrainConstants.kLinearDistanceConversionFactor);
    rightEncoder.setVelocityConversionFactor(DriveTrainConstants.kLinearDistanceConversionFactor / 60);
    leftEncoder.setVelocityConversionFactor(DriveTrainConstants.kLinearDistanceConversionFactor / 60);
    SmartDashboard.putData("Field", m_field);

    leftBackMotor.follow(leftFrontMotor);
    rightBackMotor.follow(rightFrontMotor);

    try {
      ahrs = new AHRS(SPI.Port.kMXP);
    } catch (RuntimeException ex){
      DriverStation.reportError("Error NAvx "+ ex.getMessage(), true);
    }

    m_odometry = new DifferentialDriveOdometry(ahrs.getRotation2d(), leftEncoder.getPosition(), rightEncoder.getPosition());
    m_odometry.resetPosition(ahrs.getRotation2d(), leftEncoder.getPosition(), rightEncoder.getPosition(), new Pose2d());
  }

  public void setBreakMode() {
    leftBackMotor.setIdleMode(IdleMode.kBrake);
    leftFrontMotor.setIdleMode(IdleMode.kBrake);
    rightFrontMotor.setIdleMode(IdleMode.kBrake);
    rightBackMotor.setIdleMode(IdleMode.kBrake);
  }

  public void setCoastMode() {
    leftBackMotor.setIdleMode(IdleMode.kCoast);
    leftFrontMotor.setIdleMode(IdleMode.kCoast);
    rightFrontMotor.setIdleMode(IdleMode.kCoast);
    rightBackMotor.setIdleMode(IdleMode.kCoast);
  }

  public void resetEncoders() {
    rightEncoder.setPosition(0);
    leftEncoder.setPosition(0);
  }

  public void arcadeDrive(double fwd, double rot) {
    differentialDrive.arcadeDrive(fwd, rot);
  }

  public double getRightEncoderPosition() {
    return rightEncoder.getPosition();
  }

  public double getLeftEncoderPosition() {
    return leftEncoder.getPosition();
  }

  public double getRightEncoderVelocity() {
    return rightEncoder.getVelocity();
  }

  public double getLeftEncoderVelocity() {
    return leftEncoder.getVelocity();
  }
  public double getHeading() {
    return ahrs.getAngle();
  }

  public Pose2d getPose() {
    return m_odometry.getPoseMeters();
  }

  public void resetOdometry(Pose2d pose) {
    resetEncoders();
    m_odometry.resetPosition(ahrs.getRotation2d(), leftEncoder.getPosition(), rightEncoder.getPosition(), pose);
  }

  public DifferentialDriveWheelSpeeds getWheelSpeeds() {
    return new DifferentialDriveWheelSpeeds(getLeftEncoderVelocity(), getRightEncoderVelocity());
  }

  public void tankDriveVolts(double leftVolts, double rightVolts) {
    leftFrontMotor.setVoltage(leftVolts);
    rightFrontMotor.setVoltage(rightVolts);
    differentialDrive.feed();
  }

  public double getAverageEncoderDistance() {
    return ((getLeftEncoderPosition() + getRightEncoderPosition()) / 2.0);
  }

  public RelativeEncoder getLeftEncoder() {
    return leftEncoder;
  }

  public RelativeEncoder getRightEncoder() {
    return rightEncoder;
  }

  public void setMaxOutput(double maxOutput) {
    differentialDrive.setMaxOutput(maxOutput);
  }

  public void zeroHeading() {
    ahrs.calibrate();
    ahrs.reset();
  }

  public double getTurnRate() {
    return -ahrs.getRate();
  }

  public DifferentialDriveOdometry getOdometry() {
    return m_odometry;
  }

  @Override
  public void periodic() {

    // This method will be called once per scheduler run
    m_odometry.update(ahrs.getRotation2d(), leftEncoder.getPosition(),
        rightEncoder.getPosition());
    m_field.setRobotPose(m_odometry.getPoseMeters());
    SmartDashboard.putNumber("Left encoder value meters", getLeftEncoderPosition());
    SmartDashboard.putNumber("RIGHT encoder value meters", getRightEncoderPosition());
    SmartDashboard.putNumber("Gyro heading", getHeading());
  }

}