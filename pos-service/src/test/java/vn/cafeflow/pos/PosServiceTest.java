package vn.cafeflow.pos;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@SpringBootTest @ActiveProfiles({"local","demo"}) @Transactional
class PosServiceTest {
    @Autowired PosService pos;
    @Test void mergePreservesMoneyAndReleasesSource(){
        var tables=pos.tables();var source=tables.stream().filter(t->t.name().equals("Sân vườn 2")).findFirst().orElseThrow();var target=tables.stream().filter(t->t.name().equals("VIP 5")).findFirst().orElseThrow();
        var merged=pos.move(source.order().id,target.id());assertEquals(0,new BigDecimal("130000").compareTo(merged.getTotal()));
        assertNull(pos.tables().stream().filter(t->t.id().equals(source.id())).findFirst().orElseThrow().order());
        var paid=pos.checkout(merged.id,"CASH",new BigDecimal("200000"),"admin");assertEquals(0,new BigDecimal("70000").compareTo(paid.getChange()));
        assertThrows(ResponseStatusException.class,()->pos.checkout(merged.id,"CASH",new BigDecimal("200000"),"admin"));
    }
    @Test void blocksCloseAndUnderpayment(){
        assertThrows(ResponseStatusException.class,()->pos.close(BigDecimal.ZERO,"","admin"));
        var order=pos.tables().stream().filter(t->t.order()!=null).findFirst().orElseThrow().order();
        assertThrows(ResponseStatusException.class,()->pos.checkout(order.id,"CASH",BigDecimal.ONE,"admin"));
    }
    @Test void closeReconcilesCashThenRequiresNewDay(){
        pos.tables().stream().filter(t->t.order()!=null).forEach(t->pos.cancel(t.order().id,"Khách hủy"));
        var day=pos.close(new BigDecimal("50000"),"Đủ tiền","admin");assertNotNull(day.current().closedAt);assertEquals(0,new BigDecimal("50000").compareTo(day.expectedCash()));
        assertThrows(ResponseStatusException.class,()->pos.add(1L,1L,"Cà phê",new BigDecimal("20000")));
        var next=pos.start(new BigDecimal("100000"));assertNull(next.current().closedAt);assertEquals(0,new BigDecimal("100000").compareTo(next.expectedCash()));
    }
    @Test void moveToEmptyAndDiscountCannotExceedSubtotal(){
        var source=pos.tables().stream().filter(t->t.order()!=null).findFirst().orElseThrow();var empty=pos.tables().stream().filter(t->t.order()==null).findFirst().orElseThrow();
        var moved=pos.move(source.order().id,empty.id());assertEquals(empty.id(),moved.tableId);
        assertThrows(ResponseStatusException.class,()->pos.adjust(moved.id,null,new BigDecimal("99999999"),BigDecimal.ZERO,""));
    }
}
