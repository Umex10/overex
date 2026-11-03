const priceFormat = new Intl.NumberFormat("de-AT", {
  style: "currency",
  currency: "EUR",
});

interface PriceFormatProps {
  /** Price in cent, like `price_cents` in the data model. */
  priceInCents: number;
}

export default function PriceFormat({ priceInCents }: PriceFormatProps) {
  return <>{priceFormat.format(priceInCents / 100)}</>;
}
