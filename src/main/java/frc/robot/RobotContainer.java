// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.RamseteController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.trajectory.TrajectoryUtil;
import edu.wpi.first.math.trajectory.constraint.DifferentialDriveVoltageConstraint;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import frc.robot.Constants.DriveTrainConstants;
import frc.robot.commands.DriveWithJoystickCommand;
import frc.robot.subsystems.DrivetrainSubsystem;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RamseteCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;

/**
 * This class is where the bulk of the robot should be declared. Since
 * Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in
 * the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of
 * the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  private final DrivetrainSubsystem drivetrainSubsystem = new DrivetrainSubsystem();

  private final DriveWithJoystickCommand driveWithJoystickCommand = new DriveWithJoystickCommand(drivetrainSubsystem);

  public static XboxController m_driver = new XboxController(Constants.kDriverControllerPort);

  SendableChooser<Command> chooser = new SendableChooser<>();

  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   */
  public RobotContainer() {
    // Configure the button bindings
    configureButtonBindings();
    drivetrainSubsystem.setDefaultCommand(driveWithJoystickCommand);

    Shuffleboard.getTab("Autonomous").add(chooser);

  }

  public Command loadPathplannerTrajectoryToRamseteCommand(String filename, boolean resetOdomtry) {
    
    var autoVoltageConstraint =
        new DifferentialDriveVoltageConstraint(
            new SimpleMotorFeedforward(
                DriveTrainConstants.ksVolts,
                DriveTrainConstants.kvVoltSecondsPerMeter,
                DriveTrainConstants.kaVoltSecondsSquaredPerMeter),
            DriveTrainConstants.kDriveKinematics,
            10);

    // Create config for trajectory
    TrajectoryConfig config =
        new TrajectoryConfig(
                1,
                2)
            // Add kinematics to ensure max speed is actually obeyed
            .setKinematics(DriveTrainConstants.kDriveKinematics)
            // Apply the voltage constraint
            .addConstraint(autoVoltageConstraint);

    // An example trajectory to follow.  All units in meters.
    Trajectory trajectory =
        TrajectoryGenerator.generateTrajectory(
            // Start at the origin facing the +X direction
            new Pose2d(0, 0, new Rotation2d(0)),
            // Pass through these two interior waypoints, making an 's' curve path
            List.of(new Translation2d(1, 1), new Translation2d(2, -1)),
            // End 3 meters straight ahead of where we started, facing forward
            new Pose2d(3, 0, new Rotation2d(0)),
            // Pass config
            config);
    // try {
    //   Path trajectoryPath = Filesystem.getDeployDirectory().toPath().resolve(filename);
    //   trajectory = TrajectoryUtil.fromPathweaverJson(trajectoryPath);
    // } catch (IOException exception) {
    //   DriverStation.reportError("Unable to open trajectory" + filename, exception.getStackTrace());
    //   System.out.println("Unable to read from file " + filename);
    //   return new InstantCommand();
    // }
    drivetrainSubsystem.m_field.getObject("traj").setTrajectory(trajectory);
    
    RamseteCommand ramseteCommand = new RamseteCommand(trajectory, drivetrainSubsystem::getPose,
        new RamseteController(DriveTrainConstants.kRamseteB, DriveTrainConstants.kRamseteZeta),
        new SimpleMotorFeedforward(DriveTrainConstants.ksVolts, DriveTrainConstants.kvVoltSecondsPerMeter,
            DriveTrainConstants.kaVoltSecondsSquaredPerMeter),
        DriveTrainConstants.kDriveKinematics, drivetrainSubsystem::getWheelSpeeds,
        new PIDController(DriveTrainConstants.kPDriveVel, 0, 0),
        new PIDController(DriveTrainConstants.kPDriveVel, 0, 0), drivetrainSubsystem::tankDriveVolts,
        drivetrainSubsystem);

    if (resetOdomtry) {
      return new SequentialCommandGroup(
          new InstantCommand(() -> drivetrainSubsystem.resetOdometry(trajectory.getInitialPose())), ramseteCommand);
    } else {
      return ramseteCommand.andThen(() -> drivetrainSubsystem.tankDriveVolts(0, 0));
    }

  }

  /**
   * Use this method to define your button->command mappings. Buttons can be
   * created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing
   * it to a {@link
   * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {

  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // An ExampleCommand will run in autonomous
    return loadPathplannerTrajectoryToRamseteCommand(
        "pathplanner/generatedJSON/straight.wpilib.json",
        true);
  }

  public DrivetrainSubsystem getDriveTrainSubsystem() {
    return drivetrainSubsystem;
  }

}
