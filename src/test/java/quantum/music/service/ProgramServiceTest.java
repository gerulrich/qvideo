package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.quarkus.panache.common.Page;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.ws.rs.NotFoundException;

import java.util.Collections;
import java.util.List;

import quantum.music.model.Program;
import quantum.music.model.PagedData;
import quantum.music.repository.ProgramRepository;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @InjectMocks
    ProgramService programService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProgramRepository repository;

    @Test
    void testGetPrograms_NoPrograms() {
        int level = 3, page = 0, size = 5;
        when(repository.find(anyString(), eq(level)).page(any(Page.class)).list())
            .thenReturn(Uni.createFrom().item(Collections.emptyList()));
        when(repository.count(anyString(), eq(level)))
            .thenReturn(Uni.createFrom().item(0L));

        PagedData<Program> result = programService
            .getPrograms(level, page, size)
            .await().indefinitely();

        assertTrue(result.items().isEmpty());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(0L, result.elements());
        assertEquals(0, result.total());
    }

    @Test
    void testGetPrograms_WithPrograms() {
        int level = 2, page = 1, size = 2;
        Program p1 = new Program(); p1.id = new ObjectId("60d5f484b3f1c8b1a4e8e0b1"); p1.url = "url1";
        Program p2 = new Program(); p2.id = new ObjectId("60d5f484b3f1c8b1a4e8e0b2"); p2.url = "url2";
        List<Program> programs = List.of(p1, p2);

        when(repository.find(anyString(), eq(level)).page(any(Page.class)).list())
            .thenReturn(Uni.createFrom().item(programs));
        when(repository.count(anyString(), eq(level)))
            .thenReturn(Uni.createFrom().item(5L));

        PagedData<Program> result = programService
            .getPrograms(level, page, size)
            .await().indefinitely();

        assertEquals(2, result.items().size());
        assertEquals(p1, result.items().get(0));
        assertEquals(p2, result.items().get(1));
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertEquals(5L, result.elements());
        assertEquals(3, result.total());
    }

    @Test
    void testGetProgram_Found() {
        String id = "60d5f484b3f1c8b1a4e8e0c1";
        int level = 4;
        Program program = new Program(); program.id = new ObjectId(id); program.title = "test";

        when(repository.find(anyString(), any(ObjectId.class), eq(level)).firstResult())
            .thenReturn(Uni.createFrom().item(program));

        Program result = programService
            .getProgram(id, level)
            .await().indefinitely();

        assertEquals(program, result);
    }

    @Test
    void testGetProgram_NotFound() {
        String id = "60d5f484b3f1c8b1a4e8e0c2";
        int level = 1;

        when(repository.find(anyString(), any(ObjectId.class), eq(level)).firstResult())
            .thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class,
            () -> programService.getProgram(id, level).await().indefinitely());
    }
}
