package com.google.clawminium.godconsole;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testSaveWorldButton() {
        onView(withId(R.id.btn_save)).perform(click());
        onView(withId(R.id.status_text)).check(matches(withText("Thank you for saving the world!")));
        onView(withId(R.id.main_layout)).check(matches(withBackgroundColor(Color.GREEN)));
    }

    @Test
    public void testDestroyWorldButton() {
        onView(withId(R.id.btn_destroy)).perform(click());
        onView(withId(R.id.status_text)).check(matches(withText("You just destroyed the world!")));
        onView(withId(R.id.main_layout)).check(matches(withBackgroundColor(Color.RED)));
    }

    public static Matcher<View> withBackgroundColor(final int color) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return ((ColorDrawable) item.getBackground()).getColor() == color;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with background color: " + color);
            }
        };
    }
}
