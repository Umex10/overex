"use client";

const timeFormat = new Intl.DateTimeFormat("de-AT", {
  hour: "2-digit",
  minute: "2-digit",
});

interface TimeFormatProps {
  timeToFormat: string;
}

export default function TimeFormat({ timeToFormat }: TimeFormatProps) {
  const dateOf = new Date(timeToFormat);

  return (
    <time dateTime={dateOf.toISOString()}>
      {timeFormat.format(dateOf)} Uhr
    </time>
  );
}
