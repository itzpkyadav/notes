// first case   
List<InstructorBatchList> myUniqueList = distinctList(instructorBatchLists, InstructorBatchList::getCourseId);

// second case
List<InstructorBatchList> uniqueObjects = instructorBatchLists.stream()
                                        .collect(Collectors.groupingBy((p) -> p.getCourseId())) //expression
                                        .values()
                                        .stream()
                                        .flatMap(e -> e.stream().limit(1))
                                        .collect(Collectors.toList());

  


public static <T> List<T> distinctList(List<T> list, Function<? super T, ?>... keyExtractors) {

        return list.stream().filter(distinctByKeys(keyExtractors)).collect(Collectors.toList());
}

private static <T> Predicate<T> distinctByKeys(Function<? super T, ?>... keyExtractors) {

        final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();

        return t -> {
            final List<?> keys = Arrays.stream(keyExtractors).map(ke -> ke.apply(t)).collect(Collectors.toList());

            return seen.putIfAbsent(keys, Boolean.TRUE) == null;

        };
}



List<Object> newList = categoryList.stream().distinct().collect(Collectors.toList()); // remove dublicate item from list
