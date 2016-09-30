package com.mohammedsazid.android.browse;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WebsiteVisitTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void websiteVisitTest() {
        ViewInteraction appCompatAutoCompleteTextView = onView(
                allOf(withId(R.id.addressBar_et), isDisplayed()));
        appCompatAutoCompleteTextView.perform(replaceText("google.com"), closeSoftKeyboard());

        ViewInteraction appCompatAutoCompleteTextView2 = onView(
                allOf(withId(R.id.addressBar_et), withText("google.com"), isDisplayed()));
        appCompatAutoCompleteTextView2.perform(pressImeActionButton());

        ViewInteraction textView = onView(
                allOf(withId(R.id.title_tv), withText("Google"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_bar),
                                        1),
                                1),
                        isDisplayed()));
        textView.check(matches(withText("Google")));

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
