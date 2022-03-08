package calang.types;

import java.util.List;

public class Operators {

    public static abstract class WithReturnType<T extends TypedValue<T, ?>> implements Operator<T> {
        private final Class<?> returnType;
        WithReturnType(Class<?> returnType) {
            this.returnType = returnType;
        }

        @Override
        public boolean canBeStoredIn(Class<? extends TypedValue<?, ?>> store) {
            return returnType == store;
        }
    }

    public static <T extends TypedValue<T, ?>> Operator<T> describes(
            Class<T> baseType, Class<?> returnType,
            List<Class<? extends TypedValue<?, ?>>> typeChecker
    ) {
        class Impl extends WithReturnType<T> {
            Impl() { super(returnType); }

            @Override
            public boolean doesAccept(List<Class<? extends TypedValue<?, ?>>> clz) {
                check: if(clz.size() == typeChecker.size()) {
                    for (int i = 0; i < clz.size(); i++)
                        if (clz.get(i) != typeChecker.get(i)) break check;
                    return true;
                } return false;
            }
        } return new Impl();
    }

    public static <T extends TypedValue<T, ?>, R extends TypedValue<R, ?>> Operator<T> describes(
            Class<T> baseType, Class<R> returnType, Class<? extends TypedValue<?, ?>> typeChecker
    ) {
        class Impl extends WithReturnType<T> {
            Impl() { super(returnType); }

            @Override
            public boolean doesAccept(List<Class<? extends TypedValue<?, ?>>> clz) {
                for (Class<? extends TypedValue<?, ?>> aClass : clz)
                    if (aClass != typeChecker)
                        return false;
                return true;
            }
        } return new Impl();
    }

}
