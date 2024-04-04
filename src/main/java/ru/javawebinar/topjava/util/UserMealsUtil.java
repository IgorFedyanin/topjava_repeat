package ru.javawebinar.topjava.util;

import ru.javawebinar.topjava.model.UserMeal;
import ru.javawebinar.topjava.model.UserMealWithExcess;

import java.lang.reflect.Member;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class UserMealsUtil {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<UserMeal> meals = Arrays.asList(
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 10, 0), "Завтрак", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 13, 0), "Обед", 1000),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 20, 0), "Ужин", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 0, 0), "Еда на граничное значение", 100),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 10, 0), "Завтрак", 1000),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 13, 0), "Обед", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 20, 0), "Ужин", 410)
        );

        List<UserMealWithExcess> mealsTo = filteredByCycles(meals, LocalTime.of(7, 0), LocalTime.of(12, 0), 2000);
        mealsTo.forEach(System.out::println);

        System.out.println(filteredByStreams(meals, LocalTime.of(7, 0), LocalTime.of(12, 0), 2000));

        System.out.println(getFilteredByExecutor(meals, LocalTime.of(7, 0), LocalTime.of(12, 0), 2000));
    }

    public static List<UserMealWithExcess> filteredByCycles(List<UserMeal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        // TODO return filtered list with excess. Implement by cycles
        Map<LocalDate, Integer> userMeals = new HashMap<>();
        for (UserMeal meal : meals) {
            userMeals.merge(meal.getDateTime().toLocalDate(), meal.getCalories(), Integer::sum);
        }

        ArrayList<UserMealWithExcess> userMealWithExcesses = new ArrayList<>();
        for (UserMeal meal : meals) {
            if (TimeUtil.isBetweenHalfOpen(meal.getDateTime().toLocalTime(), startTime, endTime)){
                userMealWithExcesses.add(new UserMealWithExcess(meal.getDateTime(), meal.getDescription(), meal.getCalories(), userMeals.get(meal.getDateTime().toLocalDate()) >= caloriesPerDay));
            }
        }
        return userMealWithExcesses;
    }

    public static List<UserMealWithExcess> getFilteredByExecutor(List<UserMeal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws InterruptedException, ExecutionException {
        Map<LocalDate, Integer> sumCaloriesDay = new HashMap<>();
        List<Callable<UserMealWithExcess>> tasks = new ArrayList<>();

        meals.forEach(meal->{
            sumCaloriesDay.merge(meal.getDateTime().toLocalDate(), meal.getCalories(), Integer::sum);
            if(TimeUtil.isBetweenHalfOpen(meal.getDateTime().toLocalTime(), startTime, endTime)){
                tasks.add(()->new UserMealWithExcess(meal.getDateTime(), meal.getDescription(), meal.getCalories(), sumCaloriesDay.get(meal.getDateTime().toLocalDate())>=caloriesPerDay));
            }
        });

        List<Future<UserMealWithExcess>> futures = Executors.newFixedThreadPool(4).invokeAll(tasks);
        final List<UserMealWithExcess> userMealWithExcesses = new ArrayList<>();
        for (Future<UserMealWithExcess> future : futures) {
            userMealWithExcesses.add(future.get());
        }
        return userMealWithExcesses;
    }

    public static List<UserMealWithExcess> filteredByStreams(List<UserMeal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        // TODO Implement by streams
        Map<LocalDate, Integer> userMeals = meals.stream()
                .collect(Collectors.groupingBy(p->p.getDateTime().toLocalDate(), Collectors.summingInt(UserMeal::getCalories)));


        return meals.stream().filter(p->TimeUtil.isBetweenHalfOpen(p.getDateTime().toLocalTime(), startTime, endTime))
                .map(p->new UserMealWithExcess(p.getDateTime(), p.getDescription(), caloriesPerDay, userMeals.get(p.getDateTime().toLocalDate())>=caloriesPerDay))
                .collect(Collectors.toList());
    }
}
