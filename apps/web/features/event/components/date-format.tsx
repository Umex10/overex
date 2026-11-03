"use client";

interface DateFormatProps {
  dateToFormat: string;
  weekdayType: "short" | "long";
}

export default function DateFormat(date: DateFormatProps) {
  const dateFormat = new Intl.DateTimeFormat("de-AT", {
    weekday: date.weekdayType,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  const dateOf = new Date(date.dateToFormat);

  return (
    <time dateTime={dateOf.toISOString()}>{dateFormat.format(dateOf)}</time>
  );
}
