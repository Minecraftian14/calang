package calang.types;
import java.util.List;

public interface Operator<T extends TypedValue<T, ?>> {

    boolean doesAccept(List<Class<? extends TypedValue<?, ?>>> clz);

    boolean canBeStoredIn(Class<? extends TypedValue<?, ?>> store);

}
