package me.nov.cafecompare.remapping;

public class SuspectedItem<T> {
  public T item;
  public float percent;

  public SuspectedItem(T item, float percent) {
    this.item = item;
    this.percent = percent;
  }
}
