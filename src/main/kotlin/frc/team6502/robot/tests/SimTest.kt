package frc.team6502.robot.tests

import edu.wpi.first.wpilibj.controller.SimpleMotorFeedforward
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import frc.team6502.robot.Constants
import kyberlib.command.KRobot
import kyberlib.input.controller.KXboxController
import kyberlib.math.units.extensions.feet
import kyberlib.math.units.extensions.inches
import kyberlib.mechanisms.drivetrain.DifferentialDriveConfigs
import kyberlib.mechanisms.drivetrain.DifferentialDriveTrain
import kyberlib.motorcontrol.KSimulatedESC
import kyberlib.sensors.gyros.KPigeon
import kyberlib.simulation.Simulation
import kotlin.math.PI

class SimTest : KRobot() {
    private val ff = SimpleMotorFeedforward(Constants.DRIVE_KS, Constants.DRIVE_KV, Constants.DRIVE_KA)
    private val P = 0.7
    private val leftMotor = KSimulatedESC("left").apply {
        addFeedforward(ff)
        kP = P
    }
    private val rightMotor = KSimulatedESC("right").apply {
        addFeedforward(ff)
        kP = P
    }
    private val configs = DifferentialDriveConfigs(2.inches, 1.feet)
    private val gyro = KPigeon(1)
    val driveTrain = DifferentialDriveTrain(leftMotor, rightMotor, configs, gyro)
    val controller = KXboxController(0).apply {
        rightX.apply {
            maxVal = -5 * PI
            expo = 73.0
            deadband = 0.1
        }

        // throttle
        leftY.apply {
            maxVal = -2.0
            expo = 20.0
            deadband = 0.2
        }
    }

    override fun simulationInit() {
        val ff = SimpleMotorFeedforward(Constants.DRIVE_KS, Constants.DRIVE_KV, Constants.DRIVE_KA)
        leftMotor.addFeedforward(ff)
        rightMotor.addFeedforward(ff)
        driveTrain.setupSim(Constants.DRIVE_KV, Constants.DRIVE_KA, 1.5, 0.3)  // this is a physical representation of the drivetrain
        driveTrain.drive(ChassisSpeeds(1.0, 0.0, 0.0))  // starts it driving forward
//        Simulation()
        Simulation.instance.include(driveTrain)  // this will periodically update
//        driveTrain.pose = Pose2d(2.meters, 2.meters, 0.degrees)
    }

    override fun simulationPeriodic() {
        val forward = controller.leftY.value
        val turn = controller.rightX.value
        SmartDashboard.putNumber("forward", forward)
        SmartDashboard.putNumber("turn", turn)
        driveTrain.drive(ChassisSpeeds(forward, 0.0, turn))
//        driveTrain.drive(ChassisSpeeds(0.0, 0.0, 1.0))

        Simulation.instance.field.robotPose = driveTrain.pose
    }
}