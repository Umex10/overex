import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { Celebration } from "@/components/fun/celebration";

/**
 * The fun gag is optional, but it should at least render without crashing and
 * show a celebratory subtitle. (If you delete components/fun/, delete this test
 * too.)
 */
describe("Celebration (fun gag)", () => {
  it("renders the party emoji and a subtitle", () => {
    render(<Celebration />);
    expect(screen.getByText("🎉")).toBeInTheDocument();
    // One of the goofy subtitles is shown (default before/after mount).
    expect(screen.getByText(/did it|wizard|vibes|pro|coffee/i)).toBeInTheDocument();
  });
});
