package edu.iis.mto.testreactor.dishwasher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishWasherTest {

    @Mock
    Engine engine;

    @Mock
    WaterPump waterPump;

    @Mock
    DirtFilter dirtFilter;

    @Mock
    Door door;

    private DishWasher dishWasher;
    private ProgramConfiguration programConfigurationBasic;

    @BeforeEach
    void init(){
        dishWasher = new DishWasher(waterPump,engine,dirtFilter,door);
        programConfigurationBasic = ProgramConfiguration.builder().withProgram(WashingProgram.ECO).withTabletsUsed(true).withFillLevel(FillLevel.FULL).build();
    }

    @Test
    void methodShouldReturnErrorDoorOpenWhenDoorIsOpen() {

        when(door.closed()).thenReturn(false);



        assertEquals(Status.DOOR_OPEN, dishWasher.start(programConfigurationBasic).getStatus());
    }

    @Test
    void methodShouldReturnErrorFilterWhenDirtIsOverMaxValue(){

        when(dirtFilter.capacity()).thenReturn(10.0);
        when(door.closed()).thenReturn(true);

        assertEquals(Status.ERROR_FILTER, dishWasher.start(programConfigurationBasic).getStatus());

    }

    @Test
    void methodShouldReturnErrorPumpWhenPumpPourThrowsException() throws PumpException {
        doThrow(PumpException.class).when(waterPump).pour(any());
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(60.0);

        assertEquals(Status.ERROR_PUMP, dishWasher.start(programConfigurationBasic).getStatus());
    }

    @Test
    void methodShouldReturnErrorPumpWhenPumpDrainThrowsException() throws PumpException {
        doThrow(PumpException.class).when(waterPump).drain();
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(60.0);

        assertEquals(Status.ERROR_PUMP, dishWasher.start(programConfigurationBasic).getStatus());
    }

    @Test
    void methodShouldReturnErrorProgramWhenEngineRunProgramThrowsException() throws EngineException {
        doThrow(EngineException.class).when(engine).runProgram(any());
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(60.0);

        assertEquals(Status.ERROR_PROGRAM, dishWasher.start(programConfigurationBasic).getStatus());
    }

    @Test
    void methodShouldNotReturnErrorFilterWhenTabletIsNotUsed(){
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                .withProgram(WashingProgram.ECO)
                .withTabletsUsed(false)
                .withFillLevel(FillLevel.FULL).build();

        lenient().when(dirtFilter.capacity()).thenReturn(10.0);
        when(door.closed()).thenReturn(true);

        assertEquals(Status.SUCCESS, dishWasher.start(programConfiguration).getStatus());
    }

    @Test
    void methodShouldCheckOnceIfDoorIsClosed(){

        dishWasher.start(programConfigurationBasic);

        verify(door,times(1)).closed();
    }

    @Test
    void methodShouldCheckFilterWhenTabletIsUsed(){

        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(60.0);

        dishWasher.start(programConfigurationBasic);

        verify(dirtFilter,times(1)).capacity();
    }

    @Test
    void methodShouldLockAndUnlockDoorExactlyOnce(){
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(60.0);

        dishWasher.start(programConfigurationBasic);

        verify(door,times(1)).lock();
        verify(door,times(1)).unlock();
    }

    @Test
    void methodShouldCallAllThreeWashingMethodsOnceForRinse() throws PumpException, EngineException {
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                .withProgram(WashingProgram.RINSE)
                .withTabletsUsed(true)
                .withFillLevel(FillLevel.FULL).build();

        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(60.0);

        dishWasher.start(programConfiguration);

        verify(waterPump,times(1)).pour(any());
        verify(waterPump,times(1)).drain();
        verify(engine,times(1)).runProgram(any());
    }

}
