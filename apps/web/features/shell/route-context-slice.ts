import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface RouteContextState {
  breadcrumbs: string[];
  showSearch: boolean;
}

const initialState: RouteContextState = {
  breadcrumbs: ["events"],
  showSearch: true,
};

export const routeContextSlice = createSlice({
  name: "routeContext",
  initialState,
  reducers: {
    setBreadcrumbs: (state, action: PayloadAction<string[]>) => {
      state.breadcrumbs = action.payload;
    },
    setShowSearch: (state, action: PayloadAction<boolean>) => {
      state.showSearch = action.payload;
    },
  },
});

export const { setBreadcrumbs, setShowSearch } = routeContextSlice.actions;
export default routeContextSlice.reducer;
