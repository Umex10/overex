import { apiSlice } from "@/redux/api-slice";
import {
  getAccessTkAction,
  signInAction,
  signUpAction,
} from "@/features/auth/auth-actions";
import type { AuthResponseDto } from "@/features/auth/auth-types";
import type { ApiErrorBody } from "@/types/api-error";
import type { SignInFormValues, SignUpFormValues } from "@/features/auth/auth-validation";

/**
 * Auth endpoints. They do not call the backend from the browser but go through
 * the server actions, so the refresh token stays in an HTTP-only cookie that
 * client-side JavaScript never sees.
 *
 * Errors are the backend's error body ({@link ApiErrorBody}); the pages read
 * `message` and, for sign-up, `errors` from it.
 */
const authApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    signUp: builder.mutation<AuthResponseDto, SignUpFormValues>({
      async queryFn(signUpData) {
        const res = await signUpAction(signUpData);
        return res.success ? { data: res.data } : { error: asError(res.error) };
      },
      async onQueryStarted(_arg, { dispatch, queryFulfilled }) {
        await cacheAccessTk(queryFulfilled, dispatch);
      },
    }),

    signIn: builder.mutation<AuthResponseDto, SignInFormValues>({
      async queryFn(signInData) {
        const res = await signInAction(signInData);
        return res.success ? { data: res.data } : { error: asError(res.error) };
      },
      async onQueryStarted(_arg, { dispatch, queryFulfilled }) {
        await cacheAccessTk(queryFulfilled, dispatch);
      },
    }),

    getAccessTk: builder.query<AuthResponseDto | null, undefined>({
      async queryFn() {
        const res = await getAccessTkAction();
        return res.success ? { data: res.data } : { error: asError(res.error) };
      },
      // 15 minutes — matches the access-token lifetime.
      keepUnusedDataFor: 900,
    }),
  }),
});

/**
 * The endpoints' error type is the one of the shared base query
 * (`FetchBaseQueryError`: `status` + `data`). The server actions return the
 * backend body directly, so it is passed through under the same shape the
 * pages already read (`message`, `errors`) and typed as such.
 */
function asError(body: ApiErrorBody) {
  return { status: body.status ?? 500, data: body, ...body } as {
    status: number;
    data: ApiErrorBody;
  } & ApiErrorBody;
}

/**
 * After sign-up/sign-in, puts the returned access token straight into the
 * `getAccessTk` cache, so the dashboard does not need an extra refresh call.
 * `upsert` also creates the entry when none exists yet.
 */
async function cacheAccessTk(
  queryFulfilled: Promise<{ data: AuthResponseDto }>,
  dispatch: (action: ReturnType<typeof authApi.util.upsertQueryData>) => unknown,
) {
  try {
    const { data } = await queryFulfilled;
    dispatch(authApi.util.upsertQueryData("getAccessTk", undefined, data));
  } catch {
    // Mutation failed; nothing to cache.
  }
}

export const { useSignUpMutation, useSignInMutation, useGetAccessTkQuery } =
  authApi;
